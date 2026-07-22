package com.company.blog.common.api;

import java.util.Map;

/**
 * 对外返回的统一错误体。
 *
 * <p>{@code code} 供前端按业务类型处理，{@code message} 面向使用者，{@code traceId} 用于在
 * 网关和各服务日志中关联同一次请求，{@code details} 承载安全的字段级补充信息。</p>
 */
public record ApiError(String code, String message, String traceId, Map<String, Object> details) {
    public ApiError {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
