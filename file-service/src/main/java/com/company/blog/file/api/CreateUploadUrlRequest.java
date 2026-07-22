package com.company.blog.file.api;

public record CreateUploadUrlRequest(String originalName, String contentType, long sizeBytes) {
}
