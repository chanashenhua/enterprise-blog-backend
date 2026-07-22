package com.company.blog.file.api;

public interface ObjectStorageVerifier {
    VerifiedObject verify(String objectKey, String contentType, long expectedSizeBytes);
}
