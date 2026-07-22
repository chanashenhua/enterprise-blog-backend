package com.company.blog.file.api;

public record VerifiedObject(String versionId) {
    public VerifiedObject {
        if (versionId == null || versionId.isBlank()) {
            throw new IllegalArgumentException("versionId must not be blank");
        }
    }
}
