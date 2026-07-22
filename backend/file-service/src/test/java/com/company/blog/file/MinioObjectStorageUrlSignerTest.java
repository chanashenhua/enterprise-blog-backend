package com.company.blog.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.file.api.MinioObjectStorageUrlSigner;
import com.company.blog.file.api.PresignedUpload;
import io.minio.MinioClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class MinioObjectStorageUrlSignerTest {
    @Test
    void createsBrowserReachablePostPolicyWithExactObjectSize() {
        MinioObjectStorageUrlSigner signer = new MinioObjectStorageUrlSigner(
                MinioClient.builder()
                        .endpoint("http://localhost:9000")
                        .region("us-east-1")
                        .credentials("test-access-key", "test-secret-key")
                        .build(),
                "blog-files",
                Duration.ofMinutes(10),
                "http://localhost:9000"
        );

        PresignedUpload upload = signer.createUpload(
                "files/u-author/f-1/diagram.png",
                "image/png",
                2048
        );

        String policy = new String(
                Base64.getDecoder().decode(upload.uploadFormFields().get("policy")),
                StandardCharsets.UTF_8
        );
        assertThat(upload.uploadUrl()).isEqualTo("http://localhost:9000/blog-files");
        assertThat(upload.uploadFormFields()).containsEntry("key", "files/u-author/f-1/diagram.png");
        assertThat(upload.uploadFormFields()).containsEntry("Content-Type", "image/png");
        assertThat(policy).contains("content-length-range", "2048");
    }

    @Test
    void createsDownloadUrlWithTheBrowserReachableEndpointWithoutNetworkLookup() {
        MinioObjectStorageUrlSigner signer = new MinioObjectStorageUrlSigner(
                MinioClient.builder()
                        .endpoint("http://localhost:9000")
                        .region("us-east-1")
                        .credentials("test-access-key", "test-secret-key")
                        .build(),
                "blog-files",
                Duration.ofMinutes(10),
                "http://localhost:9000"
        );

        String downloadUrl = signer.createDownloadUrl("files/u-author/f-1/diagram.png", "version-1");

        assertThat(downloadUrl).startsWith("http://localhost:9000/blog-files/files/u-author/f-1/diagram.png?");
        assertThat(downloadUrl).contains("versionId=version-1");
    }
}
