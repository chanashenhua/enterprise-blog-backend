package com.company.blog.file.api;

public interface ObjectStorageUrlSigner {
    PresignedUpload createUpload(String objectKey, String contentType, long sizeBytes);

    String createDownloadUrl(String objectKey, String versionId);
}
