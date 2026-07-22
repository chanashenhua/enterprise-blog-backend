package com.company.blog.common.api;

import java.util.Map;

public record ApiError(String code, String message, String traceId, Map<String, Object> details) {
    public ApiError {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}