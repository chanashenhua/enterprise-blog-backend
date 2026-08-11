package com.company.blog.article.api;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpArticleEngagementClient implements ArticleEngagementClient {
    private final RestClient restClient;
    private final String feedToken;

    public HttpArticleEngagementClient(
            @Value("${blog.services.stats-service-url:http://stats-service:8090}") String baseUrl,
            @Value("${blog.internal.feed-token:local-feed-token}") String feedToken
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.feedToken = feedToken;
    }

    @Override
    public List<ArticleEngagement> topArticles(int limit) {
        ArticleEngagement[] response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/stats/article-rankings")
                        .queryParam("limit", limit)
                        .build())
                .header("X-Internal-Token", feedToken)
                .retrieve()
                .body(ArticleEngagement[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }
}
