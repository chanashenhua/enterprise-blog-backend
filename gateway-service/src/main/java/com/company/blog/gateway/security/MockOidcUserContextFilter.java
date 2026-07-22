package com.company.blog.gateway.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Profile("dev")
/**
 * 仅在 {@code dev} Profile 中启用的本地身份模拟器。
 *
 * <p>它需要独立的模拟令牌，并把 {@code X-Mock-*} 转换为下游可信头后删除原头。生产环境不加载
 * 此 Bean，实际 OIDC 集成应在同一位置注入经过认证的用户上下文。</p>
 */
public class MockOidcUserContextFilter implements GlobalFilter, Ordered {
    public static final String MOCK_USER_HEADER = "X-Mock-User";
    public static final String MOCK_ROLES_HEADER = "X-Mock-Roles";
    public static final String MOCK_DEPARTMENTS_HEADER = "X-Mock-Departments";
    public static final String MOCK_TEAMS_HEADER = "X-Mock-Teams";
    public static final String MOCK_TOKEN_HEADER = "X-Mock-Token";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final String mockToken;

    public MockOidcUserContextFilter(@Value("${blog.mock-oidc.token}") String mockToken) {
        if (mockToken == null || mockToken.isBlank()) {
            throw new IllegalArgumentException("blog.mock-oidc.token must not be blank");
        }
        this.mockToken = mockToken;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders sourceHeaders = exchange.getRequest().getHeaders();
        if (!mockToken.equals(sourceHeaders.getFirst(MOCK_TOKEN_HEADER))) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        String traceId = firstNonBlank(sourceHeaders, TRACE_ID_HEADER)
                .orElseGet(() -> UUID.randomUUID().toString());

        // 再次清除可信头后才写入模拟身份，避免原始请求头与开发身份混用。
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    TrustedUserContextHeaderFilter.removeTrustedHeaders(headers);
                    copyIfPresent(sourceHeaders, headers, MOCK_USER_HEADER, TrustedUserContextHeaderFilter.USER_ID_HEADER);
                    copyIfPresent(sourceHeaders, headers, MOCK_ROLES_HEADER, TrustedUserContextHeaderFilter.USER_ROLES_HEADER);
                    copyIfPresent(sourceHeaders, headers, MOCK_DEPARTMENTS_HEADER,
                            TrustedUserContextHeaderFilter.DEPARTMENT_IDS_HEADER);
                    copyIfPresent(sourceHeaders, headers, MOCK_TEAMS_HEADER, TrustedUserContextHeaderFilter.TEAM_IDS_HEADER);
                    removeMockHeaders(headers);
                    headers.set(TRACE_ID_HEADER, traceId);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private static void copyIfPresent(HttpHeaders source, HttpHeaders target, String sourceHeader, String targetHeader) {
        firstNonBlank(source, sourceHeader).ifPresent(value -> target.set(targetHeader, value));
    }

    private static void removeMockHeaders(HttpHeaders headers) {
        headers.remove(MOCK_USER_HEADER);
        headers.remove(MOCK_ROLES_HEADER);
        headers.remove(MOCK_DEPARTMENTS_HEADER);
        headers.remove(MOCK_TEAMS_HEADER);
        headers.remove(MOCK_TOKEN_HEADER);
    }

    private static Optional<String> firstNonBlank(HttpHeaders headers, String header) {
        return Optional.ofNullable(headers.getFirst(header)).filter(value -> !value.isBlank());
    }
}
