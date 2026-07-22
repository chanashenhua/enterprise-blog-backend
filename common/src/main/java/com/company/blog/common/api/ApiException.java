package com.company.blog.common.api;

/**
 * 可预期业务失败的基础异常。
 *
 * <p>业务代码通过错误码和 HTTP 状态表达可处理的失败，而不是让调用方依赖 Java 异常类名。</p>
 */
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
