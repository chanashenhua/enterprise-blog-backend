package com.company.blog.article.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpArticleSearchIndexClient implements ArticleSearchIndexClient {
    private final RestClient restClient;
    private final String token;

    public HttpArticleSearchIndexClient(
            @Value("${blog.services.search-service-url:http://search-service:8088}") String baseUrl,
            @Value("${blog.internal.search-token:local-search-token}") String token
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.token = token;
    }

    @Override
    public void index(String payloadJson) {
        restClient.post()
                .uri("/internal/search/articles/index")
                .header("X-Internal-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payloadJson)
                .retrieve()
                .toBodilessEntity();
    }
}
