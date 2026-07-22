package com.company.blog.file;

import io.minio.MinioClient;
import java.time.Clock;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FileServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    FileValidation fileValidation(
            @Value("${blog.storage.allowed-content-types:image/png,image/jpeg,application/pdf}") String allowedContentTypes,
            @Value("${blog.storage.max-size-bytes:10485760}") long maxSizeBytes
    ) {
        Set<String> allowed = Arrays.stream(allowedContentTypes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        return new FileValidation(allowed, maxSizeBytes);
    }

    @Bean
    MinioClient storageMinioClient(
            @Value("${blog.storage.internal-endpoint}") String endpoint,
            @Value("${blog.storage.access-key}") String accessKey,
            @Value("${blog.storage.secret-key}") String secretKey,
            @Value("${blog.storage.region:us-east-1}") String region
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .region(region)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean("presigningMinioClient")
    MinioClient presigningMinioClient(
            @Value("${blog.storage.public-endpoint}") String endpoint,
            @Value("${blog.storage.access-key}") String accessKey,
            @Value("${blog.storage.secret-key}") String secretKey,
            @Value("${blog.storage.region:us-east-1}") String region
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .region(region)
                .credentials(accessKey, secretKey)
                .build();
    }
}
