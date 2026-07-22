package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class HttpPermissionCheckClient implements PermissionCheckClient {
    private final RestClient restClient;

    public HttpPermissionCheckClient(@Value("${blog.services.permission-service-url:http://permission-service:8083}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public void requirePublishAllowed(CallerContext callerContext, Article article, SubmitPublishRequest request) {
        PermissionCheckRequest permissionRequest = new PermissionCheckRequest(
                callerContext.userId(),
                callerContext.roles(),
                callerContext.departmentIds(),
                callerContext.teamIds(),
                "article.publish",
                "article",
                article.authorId(),
                request.visibilityType().toLowerCase(Locale.ROOT),
                request.targetOrgIds()
        );
        PermissionCheckResponse response = restClient.post()
                .uri("/internal/permissions/check")
                .body(permissionRequest)
                .retrieve()
                .body(PermissionCheckResponse.class);
        if (response == null || !response.allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, response == null ? "PERMISSION_DENIED" : response.reason());
        }
    }
}