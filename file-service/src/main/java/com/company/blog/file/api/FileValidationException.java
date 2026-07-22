package com.company.blog.file.api;

public class FileValidationException extends RuntimeException {
    private final String reason;

    public FileValidationException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
