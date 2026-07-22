package com.company.blog.file.api;

import java.util.Map;

public record PresignedUpload(String uploadUrl, Map<String, String> uploadFormFields) {
    public PresignedUpload {
        if (uploadUrl == null || uploadUrl.isBlank()) {
            throw new IllegalArgumentException("uploadUrl must not be blank");
        }
        uploadFormFields = Map.copyOf(uploadFormFields == null ? Map.of() : uploadFormFields);
    }
}
