package com.company.blog.gateway.security;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Profile("!dev")
public class JwtUserContextFilter implements GlobalFilter, Ordered {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final String userIdClaim;
    private final String rolesClaim;
    private final String departmentsClaim;
    private final String teamsClaim;

    public JwtUserContextFilter(
            @Value("${blog.oidc.claims.user-id:sub}") String userIdClaim,
            @Value("${blog.oidc.claims.roles:roles}") String rolesClaim,
            @Value("${blog.oidc.claims.departments:department_ids}") String departmentsClaim,
            @Value("${blog.oidc.claims.teams:team_ids}") String teamsClaim
    ) {
        this.userIdClaim = userIdClaim;
        this.rolesClaim = rolesClaim;
        this.departmentsClaim = departmentsClaim;
        this.teamsClaim = teamsClaim;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> forwardAuthenticated(exchange, chain, authentication))
                .switchIfEmpty(unauthorized(exchange));
    }

    private Mono<Void> forwardAuthenticated(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            JwtAuthenticationToken authentication
    ) {
        Map<String, Object> claims = authentication.getToken().getClaims();
        String userId = scalarClaim(claims, userIdClaim).orElse(null);
        if (userId == null || userId.isBlank()) {
            return unauthorized(exchange);
        }
        String roles = valuesClaim(claims, rolesClaim).stream()
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role)
                .collect(Collectors.joining(","));
        String departments = String.join(",", valuesClaim(claims, departmentsClaim));
        String teams = String.join(",", valuesClaim(claims, teamsClaim));
        String traceId = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        ServerHttpRequest request = TrustedUserContextHeaderFilter.copyWithMutableHeaders(
                exchange.getRequest(),
                headers -> {
                    TrustedUserContextHeaderFilter.removeTrustedHeaders(headers);
                    removeMockHeaders(headers);
                    headers.set(TrustedUserContextHeaderFilter.USER_ID_HEADER, userId);
                    setOrRemove(headers, TrustedUserContextHeaderFilter.USER_ROLES_HEADER, roles);
                    setOrRemove(headers, TrustedUserContextHeaderFilter.DEPARTMENT_IDS_HEADER, departments);
                    setOrRemove(headers, TrustedUserContextHeaderFilter.TEAM_IDS_HEADER, teams);
                    headers.set(TRACE_ID_HEADER, traceId);
                    headers.remove(HttpHeaders.AUTHORIZATION);
                }
        );
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange) {
        return Mono.defer(() -> {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        });
    }

    private static void setOrRemove(HttpHeaders headers, String name, String value) {
        if (value == null || value.isBlank()) {
            headers.remove(name);
        } else {
            headers.set(name, value);
        }
    }

    private static void removeMockHeaders(HttpHeaders headers) {
        headers.remove(MockOidcUserContextFilter.MOCK_USER_HEADER);
        headers.remove(MockOidcUserContextFilter.MOCK_ROLES_HEADER);
        headers.remove(MockOidcUserContextFilter.MOCK_DEPARTMENTS_HEADER);
        headers.remove(MockOidcUserContextFilter.MOCK_TEAMS_HEADER);
        headers.remove(MockOidcUserContextFilter.MOCK_TOKEN_HEADER);
    }

    private static Optional<String> scalarClaim(Map<String, Object> claims, String path) {
        Object value = claim(claims, path);
        return value == null ? Optional.empty() : Optional.of(value.toString()).filter(text -> !text.isBlank());
    }

    private static LinkedHashSet<String> valuesClaim(Map<String, Object> claims, String path) {
        Object value = claim(claims, path);
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> addValue(values, item));
        } else if (value != null && value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) {
                addValue(values, Array.get(value, index));
            }
        } else if (value != null) {
            for (String item : value.toString().split(",")) {
                addValue(values, item);
            }
        }
        return values;
    }

    private static void addValue(LinkedHashSet<String> values, Object raw) {
        if (raw != null && !raw.toString().isBlank()) {
            values.add(raw.toString().trim());
        }
    }

    @SuppressWarnings("unchecked")
    private static Object claim(Map<String, Object> claims, String path) {
        Object current = claims;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(segment);
        }
        return current;
    }
}
