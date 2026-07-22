package com.company.blog.file.api;

import java.time.Instant;
import java.util.Map;

public record CreateUploadUrlResponse(
        String fileId,
        String uploadUrl,
        Map<String, String> uploadFormFields,
        Instant expiresAt
) {
    public CreateUploadUrlResponse {
        uploadFormFields = Map.copyOf(uploadFormFields == null ? Map.of() : uploadFormFields);
    }
}
