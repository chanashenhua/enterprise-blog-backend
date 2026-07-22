package com.company.blog.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.file.api.BindFilesRequest;
import com.company.blog.file.api.CreateDownloadUrlResponse;
import com.company.blog.file.api.CreateUploadUrlRequest;
import com.company.blog.file.api.CreateUploadUrlResponse;
import com.company.blog.file.api.FileAccessDeniedException;
import com.company.blog.file.api.FileMetadataRepository;
import com.company.blog.file.api.FileService;
import com.company.blog.file.api.FileValidationException;
import com.company.blog.file.api.ObjectStorageUrlSigner;
import com.company.blog.file.api.ObjectStorageVerifier;
import com.company.blog.file.api.PresignedUpload;
import com.company.blog.file.api.VerifiedObject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FileServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);
    private final InMemoryFileMetadataRepository repository = new InMemoryFileMetadataRepository();
    private final RecordingObjectStorageUrlSigner signer = new RecordingObjectStorageUrlSigner();
    private final RecordingObjectStorageVerifier verifier = new RecordingObjectStorageVerifier();
    private final FileService service = new FileService(
            new FileValidation(Set.of("image/png", "image/jpeg", "application/pdf"), 10 * 1024 * 1024),
            repository,
            signer,
            verifier,
            clock,
            Duration.ofMinutes(10)
    );

    @Test
    void createsShortLivedUploadUrlAndPersistsRequiredMetadata() {
        CreateUploadUrlResponse response = service.createUploadUrl(
                "u-author",
                new CreateUploadUrlRequest("architecture.png", "image/png", 2048)
        );

        FileMetadata metadata = repository.findById(response.fileId()).orElseThrow();
        assertThat(response.uploadUrl()).startsWith("https://signed.example/upload/");
        assertThat(response.expiresAt()).isEqualTo(Instant.parse("2026-07-17T12:10:00Z"));
        assertThat(metadata.ownerId()).isEqualTo("u-author");
        assertThat(metadata.objectKey()).contains("/" + response.fileId() + "/");
        assertThat(metadata.originalName()).isEqualTo("architecture.png");
        assertThat(metadata.contentType()).isEqualTo("image/png");
        assertThat(metadata.sizeBytes()).isEqualTo(2048);
        assertThat(metadata.createdAt()).isEqualTo(Instant.parse("2026-07-17T12:00:00Z"));
        assertThat(signer.uploadContentTypes).containsExactly("image/png");
        assertThat(signer.uploadSizes).containsExactly(2048L);
        assertThat(response.uploadFormFields()).containsEntry("key", metadata.objectKey());
        assertThat(metadata.uploadVerified()).isFalse();
    }

    @Test
    void signsDownloadOnlyForFileOwnerAndNeverReturnsPermanentUrl() {
        CreateUploadUrlResponse upload = service.createUploadUrl(
                "u-author",
                new CreateUploadUrlRequest("guide.pdf", "application/pdf", 4096)
        );

        assertThatThrownBy(() -> service.createDownloadUrl("u-other", upload.fileId()))
                .isInstanceOf(FileAccessDeniedException.class);

        CreateDownloadUrlResponse download = service.createDownloadUrl("u-author", upload.fileId());

        assertThat(download.downloadUrl()).startsWith("https://signed.example/download/");
        assertThat(download.expiresAt()).isEqualTo(Instant.parse("2026-07-17T12:10:00Z"));
        assertThat(verifier.verifiedObjectKeys).containsExactly(
                repository.findById(upload.fileId()).orElseThrow().objectKey()
        );
        assertThat(repository.findById(upload.fileId()).orElseThrow().uploadVerified()).isTrue();
        assertThat(repository.findById(upload.fileId()).orElseThrow().verifiedVersionId())
                .isEqualTo("version-" + upload.fileId());
        assertThat(signer.downloadVersionIds).containsExactly("version-" + upload.fileId());
    }

    @Test
    void bindsOnlyFilesOwnedByTheRequestedAuthor() {
        CreateUploadUrlResponse upload = service.createUploadUrl(
                "u-author",
                new CreateUploadUrlRequest("diagram.jpeg", "image/jpeg", 512)
        );

        assertThat(service.bind(new BindFilesRequest(
                "u-author",
                "ARTICLE",
                "a-1",
                Set.of(upload.fileId())
        )).boundCount()).isEqualTo(1);
        assertThat(repository.bindings).contains(upload.fileId() + ":ARTICLE:a-1");
        assertThat(verifier.verifiedObjectKeys).containsExactly(
                repository.findById(upload.fileId()).orElseThrow().objectKey()
        );
    }

    @Test
    void doesNotIssueDownloadOrBindWhenStoredObjectFailsVerification() {
        CreateUploadUrlResponse upload = service.createUploadUrl(
                "u-author",
                new CreateUploadUrlRequest("diagram.png", "image/png", 512)
        );
        verifier.rejectUploads = true;

        assertThatThrownBy(() -> service.createDownloadUrl("u-author", upload.fileId()))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("UPLOADED_FILE_CONTENT_MISMATCH");
        assertThatThrownBy(() -> service.bind(new BindFilesRequest(
                "u-author",
                "ARTICLE",
                "a-1",
                Set.of(upload.fileId())
        )))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("UPLOADED_FILE_CONTENT_MISMATCH");

        assertThat(signer.downloadObjectKeys).isEmpty();
        assertThat(repository.bindings).isEmpty();
    }

    private static final class RecordingObjectStorageUrlSigner implements ObjectStorageUrlSigner {
        private final java.util.List<String> uploadContentTypes = new java.util.ArrayList<>();
        private final java.util.List<Long> uploadSizes = new java.util.ArrayList<>();
        private final java.util.List<String> downloadObjectKeys = new java.util.ArrayList<>();
        private final java.util.List<String> downloadVersionIds = new java.util.ArrayList<>();

        @Override
        public PresignedUpload createUpload(String objectKey, String contentType, long sizeBytes) {
            uploadContentTypes.add(contentType);
            uploadSizes.add(sizeBytes);
            return new PresignedUpload(
                    "https://signed.example/upload/" + objectKey,
                    java.util.Map.of("key", objectKey, "Content-Type", contentType)
            );
        }

        @Override
        public String createDownloadUrl(String objectKey, String versionId) {
            downloadObjectKeys.add(objectKey);
            downloadVersionIds.add(versionId);
            return "https://signed.example/download/" + objectKey;
        }
    }

    private static final class RecordingObjectStorageVerifier implements ObjectStorageVerifier {
        private final java.util.List<String> verifiedObjectKeys = new java.util.ArrayList<>();
        private boolean rejectUploads;

        @Override
        public VerifiedObject verify(String objectKey, String contentType, long expectedSizeBytes) {
            if (rejectUploads) {
                throw new FileValidationException("UPLOADED_FILE_CONTENT_MISMATCH");
            }
            verifiedObjectKeys.add(objectKey);
            return new VerifiedObject("version-" + objectKey.split("/")[2]);
        }
    }

    private static final class InMemoryFileMetadataRepository implements FileMetadataRepository {
        private final HashMap<String, FileMetadata> metadata = new HashMap<>();
        private final Set<String> bindings = new HashSet<>();

        @Override
        public FileMetadata save(FileMetadata fileMetadata) {
            metadata.put(fileMetadata.id(), fileMetadata);
            return fileMetadata;
        }

        @Override
        public Optional<FileMetadata> findById(String fileId) {
            return Optional.ofNullable(metadata.get(fileId));
        }

        @Override
        public void markUploadVerified(String fileId, String versionId) {
            metadata.computeIfPresent(fileId, (ignored, fileMetadata) -> fileMetadata.verified(versionId));
        }

        @Override
        public void bind(String fileId, String resourceType, String resourceId) {
            bindings.add(fileId + ":" + resourceType + ":" + resourceId);
        }
    }
}
