package com.company.blog.search.service;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.index.ArticleSearchDocument;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpPermissionCheckClient implements PermissionCheckClient {
    private final RestClient client;
    private final String token;

    public HttpPermissionCheckClient(
            @Value("${blog.services.article-service-url:http://article-service:8084}") String baseUrl,
            @Value("${blog.internal.search-token:local-search-token}") String token,
            RestClient.Builder builder) {
        this.client = builder.baseUrl(baseUrl).build();
        this.token = token;
    }

    @Override
    public boolean canRead(UserContext user, ArticleSearchDocument document) {
        return readableIds(user, List.of(document)).contains(document.articleId());
    }

    @Override
    public Set<String> readableIds(UserContext user, List<ArticleSearchDocument> documents) {
        String[] ids = client.post().uri("/internal/articles/search-access")
                .header("X-Internal-Token", token)
                .header("X-User-Id", user.userId())
                .header("X-User-Roles", String.join(",", user.roles()))
                .header("X-Department-Ids", String.join(",", user.departmentIds()))
                .header("X-Team-Ids", String.join(",", user.teamIds()))
                .body(documents.stream().map(document -> Map.of(
                        "articleId", document.articleId(), "revision", document.revision())).toList())
                .retrieve().body(String[].class);
        if (ids == null) throw new IllegalStateException("Article authorization unavailable");
        return Set.copyOf(Arrays.asList(ids));
    }
}
