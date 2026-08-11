package com.company.blog.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class TrustedUserContextHeaderFilterTest {
    @Test
    void removesCallerSuppliedUserContextWhileLeavingMockHeadersForTheDevFilter() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/files/upload-url")
                .header("X-User-Id", "spoofed-user")
                .header("X-User-Roles", "ADMIN")
                .header("X-Department-Ids", "d-1")
                .header("X-Team-Ids", "t-1")
                .header("X-Mock-User", "u-dev")
                .build();
        TrustedUserContextHeaderFilter filter = new TrustedUserContextHeaderFilter();
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        ServerHttpRequest requestWithReadOnlyHeaders = new ServerHttpRequestDecorator(request) {
            private final HttpHeaders readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(request.getHeaders());

            @Override
            public HttpHeaders getHeaders() {
                return readOnlyHeaders;
            }
        };
        ServerWebExchange exchange = MockServerWebExchange.from(request)
                .mutate()
                .request(requestWithReadOnlyHeaders)
                .build();

        filter.filter(exchange, next -> {
            captured.set(next);
            return Mono.empty();
        }).block();

        HttpHeaders headers = captured.get().getRequest().getHeaders();
        assertThat(headers.containsKey("X-User-Id")).isFalse();
        assertThat(headers.containsKey("X-User-Roles")).isFalse();
        assertThat(headers.containsKey("X-Department-Ids")).isFalse();
        assertThat(headers.containsKey("X-Team-Ids")).isFalse();
        assertThat(headers.getFirst("X-Mock-User")).isEqualTo("u-dev");
    }
}
