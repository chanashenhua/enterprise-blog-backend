package com.company.blog.file.api;

public class FileAccessDeniedException extends RuntimeException {
    public FileAccessDeniedException(String fileId) {
        super("File access is not allowed: " + fileId);
    }
}
