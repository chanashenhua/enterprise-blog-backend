package com.company.blog.file.api;

import java.time.Instant;

public record CreateDownloadUrlResponse(String fileId, String downloadUrl, Instant expiresAt) {
}
