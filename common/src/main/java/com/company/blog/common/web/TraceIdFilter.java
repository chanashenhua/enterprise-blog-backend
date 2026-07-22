package com.company.blog.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 为 Servlet 服务建立并传播请求追踪 ID。
 *
 * <p>优先沿用上游 {@code X-Trace-Id}，缺失时生成新值；同时写入响应头和 SLF4J MDC，使日志能按请求聚合。</p>
 */
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // 允许网关将同一个追踪 ID 贯穿到多个微服务，便于排查跨服务调用链。
        String traceId = Optional.ofNullable(request.getHeader(TRACE_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        response.setHeader(TRACE_ID_HEADER, traceId);
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 线程会被容器复用，必须清理 MDC，避免下一次请求串入旧的 traceId。
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }
}
