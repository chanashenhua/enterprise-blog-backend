package com.company.blog.file.api;

import com.company.blog.file.FileMetadata;
import com.company.blog.file.FileValidation;
import com.company.blog.file.FileValidationResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 文件访问的应用服务。
 *
 * <p>上传 URL 只表示“允许尝试上传”，不表示文件可信。首次下载或业务绑定时必须调用对象存储
 * 校验器，比对大小、类型、版本和实际内容后，才把文件视为可用。</p>
 */
public class FileService {
    private final FileValidation validation;
    private final FileMetadataRepository metadataRepository;
    private final ObjectStorageUrlSigner urlSigner;
    private final ObjectStorageVerifier objectStorageVerifier;
    private final Clock clock;
    private final Duration presignedUrlExpiry;

    public FileService(
            FileValidation validation,
            FileMetadataRepository metadataRepository,
            ObjectStorageUrlSigner urlSigner,
            ObjectStorageVerifier objectStorageVerifier,
            Clock clock,
            @Value("${blog.storage.presigned-url-expiry:10m}") Duration presignedUrlExpiry
    ) {
        this.validation = validation;
        this.metadataRepository = metadataRepository;
        this.urlSigner = urlSigner;
        this.objectStorageVerifier = objectStorageVerifier;
        this.clock = clock;
        this.presignedUrlExpiry = presignedUrlExpiry;
    }

    public CreateUploadUrlResponse createUploadUrl(String ownerId, CreateUploadUrlRequest request) {
        requireText(ownerId, "X-User-Id");
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        // 先按声明的文件名、类型和大小拦截明显不合规的上传请求。
        FileValidationResult validationResult = validation.validate(
                request.originalName(),
                request.contentType(),
                request.sizeBytes()
        );
        if (!validationResult.allowed()) {
            throw new FileValidationException(validationResult.reason());
        }

        Instant createdAt = clock.instant();
        String fileId = UUID.randomUUID().toString();
        String objectKey = "files/" + safePathSegment(ownerId) + "/" + fileId + "/" + safeFilename(request.originalName());
        FileMetadata metadata = new FileMetadata(
                fileId,
                ownerId,
                objectKey,
                request.originalName(),
                request.contentType().trim().toLowerCase(Locale.ROOT),
                request.sizeBytes(),
                createdAt
        );
        PresignedUpload upload = urlSigner.createUpload(objectKey, metadata.contentType(), metadata.sizeBytes());
        metadataRepository.save(metadata);
        return new CreateUploadUrlResponse(
                fileId,
                upload.uploadUrl(),
                upload.uploadFormFields(),
                createdAt.plus(presignedUrlExpiry)
        );
    }

    public CreateDownloadUrlResponse createDownloadUrl(String requesterId, String fileId) {
        requireText(requesterId, "X-User-Id");
        FileMetadata metadata = find(fileId);
        if (!metadata.ownerId().equals(requesterId)) {
            throw new FileAccessDeniedException(fileId);
        }
        // 不为未经验证的对象签发下载地址，防止客户端声明图片却上传任意字节内容。
        FileMetadata verifiedMetadata = ensureUploadVerified(metadata);
        Instant issuedAt = clock.instant();
        return new CreateDownloadUrlResponse(
                verifiedMetadata.id(),
                urlSigner.createDownloadUrl(verifiedMetadata.objectKey(), verifiedMetadata.verifiedVersionId()),
                issuedAt.plus(presignedUrlExpiry)
        );
    }

    @Transactional
    /**
     * 将文件绑定到文章等资源。绑定前再次确认所有权和对象内容，避免“先传后换”或越权引用。
     */
    public BindFilesResponse bind(BindFilesRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        requireText(request.ownerId(), "ownerId");
        requireText(request.resourceType(), "resourceType");
        requireText(request.resourceId(), "resourceId");
        Set<String> fileIds = request.fileIds();
        if (fileIds.isEmpty()) {
            throw new IllegalArgumentException("fileIds must not be empty");
        }

        for (String fileId : fileIds) {
            FileMetadata metadata = find(fileId);
            if (!metadata.ownerId().equals(request.ownerId())) {
                throw new FileAccessDeniedException(fileId);
            }
            ensureUploadVerified(metadata);
            metadataRepository.bind(fileId, request.resourceType(), request.resourceId());
        }
        return new BindFilesResponse(fileIds.size());
    }

    private FileMetadata find(String fileId) {
        return metadataRepository.findById(fileId)
                .orElseThrow(() -> new StoredFileNotFoundException(fileId));
    }

    private FileMetadata ensureUploadVerified(FileMetadata metadata) {
        if (metadata.uploadVerified()) {
            return metadata;
        }
        // 保存对象版本号，后续下载 URL 精确指向已验证版本，而不是可能被覆盖的新版本。
        VerifiedObject verifiedObject = objectStorageVerifier.verify(
                metadata.objectKey(),
                metadata.contentType(),
                metadata.sizeBytes()
        );
        metadataRepository.markUploadVerified(metadata.id(), verifiedObject.versionId());
        return metadata.verified(verifiedObject.versionId());
    }

    private static String safeFilename(String filename) {
        String sanitized = filename.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "file" : sanitized;
    }

    private static String safePathSegment(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
