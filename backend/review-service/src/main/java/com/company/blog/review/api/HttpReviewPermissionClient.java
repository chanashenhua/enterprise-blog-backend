package com.company.blog.review.api;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class HttpReviewPermissionClient implements ReviewPermissionClient {
    private final RestClient restClient;

    public HttpReviewPermissionClient(@Value("${blog.services.permission-service-url:http://permission-service:8083}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public void requireReviewAllowed(HttpHeaders headers) {
        PermissionCheckRequest request = new PermissionCheckRequest(
                headers.getFirst("X-User-Id"),
                commaSeparated(headers.getFirst("X-User-Roles")),
                commaSeparated(headers.getFirst("X-Department-Ids")),
                commaSeparated(headers.getFirst("X-Team-Ids")),
                "article.review",
                "review_ticket",
                null,
                "company",
                Set.of()
        );
        PermissionCheckResponse response = restClient.post()
                .uri("/internal/permissions/check")
                .body(request)
                .retrieve()
                .body(PermissionCheckResponse.class);
        if (response == null || !response.allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, response == null ? "PERMISSION_DENIED" : response.reason());
        }
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