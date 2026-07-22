package com.company.blog.file.api;

public class StoredFileNotFoundException extends RuntimeException {
    public StoredFileNotFoundException(String fileId) {
        super("File not found: " + fileId);
    }
}
