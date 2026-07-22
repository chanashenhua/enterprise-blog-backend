package com.company.blog.search.service;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.index.ArticleSearchDocument;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpPermissionCheckClient implements PermissionCheckClient {
    private final RestClient restClient;

    public HttpPermissionCheckClient(@Value("${blog.services.permission-service-url:http://permission-service:8083}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public boolean canRead(UserContext user, ArticleSearchDocument document) {
        PermissionCheckResponse response = restClient.post()
                .uri("/internal/permissions/check")
                .body(Map.of(
                        "userId", user.userId(),
                        "roles", user.roles(),
                        "departmentIds", user.departmentIds(),
                        "teamIds", user.teamIds(),
                        "action", "article.read",
                        "resourceType", "article",
                        "resourceOwnerId", document.authorId(),
                        "visibilityType", document.visibilityType().toLowerCase(),
                        "targetOrgIds", document.targetOrgIds()
                ))
                .retrieve()
                .body(PermissionCheckResponse.class);
        return response != null && response.allowed();
    }

    private record PermissionCheckResponse(boolean allowed, String reason) {
    }
}
