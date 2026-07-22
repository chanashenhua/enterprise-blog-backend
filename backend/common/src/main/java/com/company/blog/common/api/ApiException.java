package com.company.blog.common.api;

public class ApiException extends RuntimeException {
    private final String code;
    private final int httpStatus;

    public ApiException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }
}