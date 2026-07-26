package com.company.blog.gateway.security;

import java.util.ArrayList;
import java.util.function.Consumer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
/**
 * 清除客户端自行携带的可信身份头。
 *
 * <p>下游服务只信任网关在认证后注入的 {@code X-User-*} 头；本过滤器先运行，确保外部请求
 * 不能伪造用户、角色或组织范围。</p>
 */
public class TrustedUserContextHeaderFilter implements GlobalFilter, Ordered {
    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLES_HEADER = "X-User-Roles";
    static final String DEPARTMENT_IDS_HEADER = "X-Department-Ids";
    static final String TEAM_IDS_HEADER = "X-Team-Ids";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitizedRequest = copyWithMutableHeaders(
                exchange.getRequest(),
                TrustedUserContextHeaderFilter::removeTrustedHeaders
        );
        return chain.filter(exchange.mutate().request(sanitizedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    static void removeTrustedHeaders(HttpHeaders headers) {
        headers.remove(USER_ID_HEADER);
        headers.remove(USER_ROLES_HEADER);
        headers.remove(DEPARTMENT_IDS_HEADER);
        headers.remove(TEAM_IDS_HEADER);
    }

    static ServerHttpRequest copyWithMutableHeaders(
            ServerHttpRequest request,
            Consumer<HttpHeaders> headerCustomizer
    ) {
        HttpHeaders mutableHeaders = new HttpHeaders();
        request.getHeaders().forEach((name, values) ->
                mutableHeaders.put(name, new ArrayList<>(values))
        );
        headerCustomizer.accept(mutableHeaders);
        HttpHeaders readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(mutableHeaders);

        return new ServerHttpRequestDecorator(request) {
            @Override
            public HttpHeaders getHeaders() {
                return readOnlyHeaders;
            }
        };
    }
}
