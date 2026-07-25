package com.company.blog.comment.api;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;

public record CallerIdentity(String userId, Set<String> roles) {
    public CallerIdentity {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public static CallerIdentity from(HttpHeaders headers) {
        return new CallerIdentity(
                headers.getFirst("X-User-Id"),
                commaSeparated(headers.getFirst("X-User-Roles"))
        );
    }

    public boolean admin() {
        return roles.contains("ADMIN");
    }

    private static Set<String> commaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
