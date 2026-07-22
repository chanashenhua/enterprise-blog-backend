package com.company.blog.file;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.file.api.FileController;
import com.company.blog.file.api.FileExceptionHandler;
import com.company.blog.file.api.FileMetadataRepository;
import com.company.blog.file.api.FileService;
import com.company.blog.file.api.ObjectStorageUrlSigner;
import com.company.blog.file.api.PresignedUpload;
import com.company.blog.file.api.VerifiedObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FileControllerTest {
    private final InMemoryFileMetadataRepository repository = new InMemoryFileMetadataRepository();
    private final FileService service = new FileService(
            new FileValidation(Set.of("image/png", "image/jpeg", "application/pdf"), 10 * 1024 * 1024),
            repository,
            new SigningUrlSigner(),
            (objectKey, contentType, expectedSizeBytes) -> new VerifiedObject("version-1"),
            Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC),
            Duration.ofMinutes(10)
    );
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new FileController(service, "test-file-token"),
            new FileExceptionHandler()
    ).setMessageConverters(new MappingJackson2HttpMessageConverter(
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    )).build();

    @Test
    void createsUploadUrlFromGatewayUserContext() throws Exception {
        mvc.perform(post("/api/files/upload-url")
                        .header("X-User-Id", "u-author")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalName":"diagram.png","contentType":"image/png","sizeBytes":1024}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").isNotEmpty())
                .andExpect(jsonPath("$.uploadUrl").value(org.hamcrest.Matchers.startsWith("https://signed.example/upload/")))
                .andExpect(jsonPath("$.uploadFormFields.key").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").value("2026-07-17T12:10:00Z"));
    }

    @Test
    void rejectsInternalBindingWithInvalidToken() throws Exception {
        mvc.perform(post("/internal/files/bind")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerId":"u-author","resourceType":"ARTICLE","resourceId":"a-1","fileIds":["f-1"]}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FILE_ACCESS_DENIED"));
    }

    private static final class SigningUrlSigner implements ObjectStorageUrlSigner {
        @Override
        public PresignedUpload createUpload(String objectKey, String contentType, long sizeBytes) {
            return new PresignedUpload(
                    "https://signed.example/upload/" + objectKey,
                    java.util.Map.of("key", objectKey, "Content-Type", contentType)
            );
        }

        @Override
        public String createDownloadUrl(String objectKey, String versionId) {
            return "https://signed.example/download/" + objectKey;
        }
    }

    private static final class InMemoryFileMetadataRepository implements FileMetadataRepository {
        private final HashMap<String, FileMetadata> metadata = new HashMap<>();

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
        }
    }
}
