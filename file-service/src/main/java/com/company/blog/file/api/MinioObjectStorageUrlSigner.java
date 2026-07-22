package com.company.blog.file.api;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.http.Method;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class MinioObjectStorageUrlSigner implements ObjectStorageUrlSigner {
    private final MinioClient minioClient;
    private final String bucket;
    private final int expirySeconds;
    private final String publicEndpoint;

    public MinioObjectStorageUrlSigner(
            @Qualifier("presigningMinioClient") MinioClient minioClient,
            @Value("${blog.storage.bucket:blog-files}") String bucket,
            @Value("${blog.storage.presigned-url-expiry:10m}") Duration expiry,
            @Value("${blog.storage.public-endpoint}") String publicEndpoint
    ) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.expirySeconds = Math.toIntExact(expiry.toSeconds());
        this.publicEndpoint = trimTrailingSlash(publicEndpoint);
        if (expirySeconds <= 0 || expirySeconds > TimeUnit.DAYS.toSeconds(7)) {
            throw new IllegalArgumentException("presigned URL expiry must be between 1 second and 7 days");
        }
    }

    @Override
    public PresignedUpload createUpload(String objectKey, String contentType, long sizeBytes) {
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive");
        }
        try {
            PostPolicy policy = new PostPolicy(
                    bucket,
                    ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(expirySeconds)
            );
            policy.addEqualsCondition("key", objectKey);
            policy.addEqualsCondition("Content-Type", contentType);
            policy.addContentLengthRangeCondition(sizeBytes, sizeBytes);

            Map<String, String> formFields = new LinkedHashMap<>(minioClient.getPresignedPostFormData(policy));
            formFields.put("key", objectKey);
            formFields.put("Content-Type", contentType);
            return new PresignedUpload(publicEndpoint + "/" + bucket, formFields);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create object storage upload policy", ex);
        }
    }

    @Override
    public String createDownloadUrl(String objectKey, String versionId) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .versionId(versionId)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create object storage URL", ex);
        }
    }

    private static String trimTrailingSlash(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("public object storage endpoint must not be blank");
        }
        return endpoint.replaceAll("/+$", "");
    }
}
