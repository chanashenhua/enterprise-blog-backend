package com.company.blog.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class JwtUserContextFilterTest {
    @Test
    void convertsVerifiedJwtClaimsIntoTrustedDownstreamHeaders() {
        Jwt jwt = Jwt.withTokenValue("verified-token")
                .header("alg", "RS256")
                .subject("u-oidc")
                .claim("realm_access", Map.of("roles", List.of("ROLE_AUTHOR", "reader")))
                .claim("departments", List.of("d-platform"))
                .claim("teams", "t-search,t-platform")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/articles")
                .header(HttpHeaders.AUTHORIZATION, "Bearer verified-token")
                .header("X-User-Id", "spoofed-user")
                .header("X-Mock-User", "mock-user")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request)
                .mutate()
                .principal(Mono.just(authentication))
                .build();
        JwtUserContextFilter filter = new JwtUserContextFilter(
                "sub",
                "realm_access.roles",
                "departments",
                "teams"
        );
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, next -> {
            captured.set(next);
            return Mono.empty();
        }).block();

        HttpHeaders headers = captured.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("u-oidc");
        assertThat(headers.getFirst("X-User-Roles")).isEqualTo("AUTHOR,READER");
        assertThat(headers.getFirst("X-Department-Ids")).isEqualTo("d-platform");
        assertThat(headers.getFirst("X-Team-Ids")).isEqualTo("t-search,t-platform");
        assertThat(headers.getFirst("X-Trace-Id")).isNotBlank();
        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(headers.containsKey("X-Mock-User")).isFalse();
    }

    @Test
    void rejectsJwtWithoutTheConfiguredUserIdentifierClaim() {
        Jwt jwt = Jwt.withTokenValue("missing-sub")
                .header("alg", "RS256")
                .claim("roles", List.of("READER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        ServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/articles").build())
                .mutate()
                .principal(Mono.just(new JwtAuthenticationToken(jwt)))
                .build();
        AtomicBoolean chainCalled = new AtomicBoolean();

        new JwtUserContextFilter("sub", "roles", "departments", "teams")
                .filter(exchange, next -> {
                    chainCalled.set(true);
                    return Mono.empty();
                })
                .block();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
        assertThat(chainCalled).isFalse();
    }
}
