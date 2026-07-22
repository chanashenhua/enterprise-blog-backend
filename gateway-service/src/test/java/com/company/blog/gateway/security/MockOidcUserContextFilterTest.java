package com.company.blog.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class MockOidcUserContextFilterTest {
    @Test
    void addsUserContextHeadersFromMockOidcHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/articles")
                .header("X-Mock-Token", "test-token")
                .header("X-Mock-User", "u-1")
                .header("X-Mock-Roles", "AUTHOR,READER")
                .header("X-Mock-Departments", "d-1")
                .header("X-Mock-Teams", "t-1")
                .header("X-User-Id", "spoofed-user")
                .build();

        MockOidcUserContextFilter filter = new MockOidcUserContextFilter("test-token");

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, next -> {
            captured.set(next);
            return Mono.empty();
        }).block();

        HttpHeaders headers = captured.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("u-1");
        assertThat(headers.getFirst("X-User-Roles")).isEqualTo("AUTHOR,READER");
        assertThat(headers.getFirst("X-Department-Ids")).isEqualTo("d-1");
        assertThat(headers.getFirst("X-Team-Ids")).isEqualTo("t-1");
        assertThat(headers.getFirst("X-Trace-Id")).isNotBlank();
        assertThat(headers.containsKey("X-Mock-User")).isFalse();
        assertThat(headers.containsKey("X-Mock-Roles")).isFalse();
        assertThat(headers.containsKey("X-Mock-Departments")).isFalse();
        assertThat(headers.containsKey("X-Mock-Teams")).isFalse();
        assertThat(headers.containsKey("X-Mock-Token")).isFalse();
    }

    @Test
    void rejectsMockHeadersWithoutTheConfiguredToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/articles")
                .header("X-Mock-Token", "wrong-token")
                .header("X-Mock-User", "u-1")
                .build();
        MockOidcUserContextFilter filter = new MockOidcUserContextFilter("test-token");
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.filter(exchange, next -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
        assertThat(chainCalled).isFalse();
    }
}
