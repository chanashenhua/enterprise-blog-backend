package com.company.blog.file;

import java.time.Instant;

public record FileMetadata(
        String id,
        String ownerId,
        String objectKey,
        String originalName,
        String contentType,
        long sizeBytes,
        Instant createdAt,
        boolean uploadVerified,
        String verifiedVersionId
) {
    public FileMetadata {
        requireText(id, "id");
        requireText(ownerId, "ownerId");
        requireText(objectKey, "objectKey");
        requireText(originalName, "originalName");
        requireText(contentType, "contentType");
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (uploadVerified && (verifiedVersionId == null || verifiedVersionId.isBlank())) {
            throw new IllegalArgumentException("verifiedVersionId must be present for a verified upload");
        }
        if (!uploadVerified) {
            verifiedVersionId = null;
        }
    }

    public FileMetadata(
            String id,
            String ownerId,
            String objectKey,
            String originalName,
            String contentType,
            long sizeBytes,
            Instant createdAt
    ) {
        this(id, ownerId, objectKey, originalName, contentType, sizeBytes, createdAt, false, null);
    }

    public FileMetadata verified(String versionId) {
        return uploadVerified ? this : new FileMetadata(
                id,
                ownerId,
                objectKey,
                originalName,
                contentType,
                sizeBytes,
                createdAt,
                true,
                versionId
        );
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
