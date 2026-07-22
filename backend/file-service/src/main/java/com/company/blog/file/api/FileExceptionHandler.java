package com.company.blog.file.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FileExceptionHandler {
    @ExceptionHandler(FileValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse validation(FileValidationException ex) {
        return new ErrorResponse(ex.reason(), ex.getMessage());
    }

    @ExceptionHandler(StoredFileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse notFound(StoredFileNotFoundException ex) {
        return new ErrorResponse("FILE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(FileAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse forbidden(FileAccessDeniedException ex) {
        return new ErrorResponse("FILE_ACCESS_DENIED", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse invalidRequest(IllegalArgumentException ex) {
        return new ErrorResponse("INVALID_FILE_REQUEST", ex.getMessage());
    }

    public record ErrorResponse(String code, String message) {
    }
}
