package com.company.blog.file.api;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
/**
 * 对已上传的 MinIO 对象做可信性复核。
 *
 * <p>除了对象元数据，还读取对象字节并按格式解析。这样客户端即使伪造扩展名或 MIME 类型，也不能
 * 将任意内容作为受支持的图片或 PDF 绑定到文章。</p>
 */
public class MinioObjectStorageVerifier implements ObjectStorageVerifier {
    private final MinioClient minioClient;
    private final String bucket;
    private final UploadedContentValidator contentValidator;

    public MinioObjectStorageVerifier(
            @Qualifier("storageMinioClient") MinioClient minioClient,
            @Value("${blog.storage.bucket:blog-files}") String bucket,
            UploadedContentValidator contentValidator
    ) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.contentValidator = contentValidator;
    }

    @Override
    public VerifiedObject verify(String objectKey, String contentType, long expectedSizeBytes) {
        try {
            StatObjectResponse object = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(objectKey).build()
            );
            if (object.size() != expectedSizeBytes) {
                throw new FileValidationException("UPLOADED_FILE_SIZE_MISMATCH");
            }
            if (!normalizedContentType(contentType).equals(normalizedContentType(object.contentType()))) {
                throw new FileValidationException("UPLOADED_FILE_TYPE_MISMATCH");
            }
            // 启用版本号后，验证和后续下载绑定到同一个不可变对象版本。
            String versionId = requireVersionId(object.versionId());

            try (GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .versionId(versionId)
                            .build()
            )) {
                byte[] content = response.readAllBytes();
                if (content.length != expectedSizeBytes) {
                    throw new FileValidationException("UPLOADED_FILE_SIZE_MISMATCH");
                }
                contentValidator.validate(normalizedContentType(contentType), content);
            }
            return new VerifiedObject(versionId);
        } catch (FileValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FileValidationException("UPLOADED_FILE_NOT_AVAILABLE");
        }
    }

    private static String normalizedContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parameterStart = contentType.indexOf(';');
        return contentType.substring(0, parameterStart < 0 ? contentType.length() : parameterStart)
                .trim()
                .toLowerCase(java.util.Locale.ROOT);
    }

    private static String requireVersionId(String versionId) {
        if (versionId == null || versionId.isBlank()) {
            throw new FileValidationException("UPLOADED_FILE_VERSION_UNAVAILABLE");
        }
        return versionId;
    }
}
