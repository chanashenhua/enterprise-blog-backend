package com.company.blog.file;

public record FileValidationResult(boolean allowed, String reason) {
    static FileValidationResult accepted() {
        return new FileValidationResult(true, null);
    }

    static FileValidationResult rejected(String reason) {
        return new FileValidationResult(false, reason);
    }
}
