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
        requireAllowed(
                callerContext,
                article,
                "article.publish",
                request.visibilityType().toLowerCase(Locale.ROOT),
                request.targetOrgIds()
        );
    }

    @Override
    public void requireReadAllowed(CallerContext callerContext, Article article) {
        requireAllowed(
                callerContext,
                article,
                "article.read",
                visibilityType(article),
                article.visibilityTargetIds()
        );
    }

    @Override
    public void requireEditAllowed(CallerContext callerContext, Article article) {
        requireAllowed(callerContext, article, "article.edit", visibilityType(article), article.visibilityTargetIds());
    }

    @Override
    public void requireWithdrawAllowed(CallerContext callerContext, Article article) {
        requireAllowed(
                callerContext,
                article,
                "article.withdraw",
                visibilityType(article),
                article.visibilityTargetIds()
        );
    }

    @Override
    public void requireDeleteAllowed(CallerContext callerContext, Article article) {
        requireAllowed(callerContext, article, "article.delete", visibilityType(article), article.visibilityTargetIds());
    }

    private void requireAllowed(
            CallerContext callerContext,
            Article article,
            String action,
            String visibilityType,
            java.util.Set<String> targetOrgIds
    ) {
        PermissionCheckRequest permissionRequest = new PermissionCheckRequest(
                callerContext.userId(),
                callerContext.roles(),
                callerContext.departmentIds(),
                callerContext.teamIds(),
                action,
                "article",
                article.authorId(),
                visibilityType,
                targetOrgIds
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

    private static String visibilityType(Article article) {
        return article.visibilityType() == null
                ? "company"
                : article.visibilityType().name().toLowerCase(Locale.ROOT);
    }
}
