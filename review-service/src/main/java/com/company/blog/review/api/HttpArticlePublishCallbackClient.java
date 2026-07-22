package com.company.blog.review.api;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpArticlePublishCallbackClient implements ArticlePublishCallbackClient {
    private final RestClient restClient;
    private final String reviewToken;

    public HttpArticlePublishCallbackClient(
            @Value("${blog.services.article-service-url:http://article-service:8084}") String baseUrl,
            @Value("${blog.internal.review-token:local-review-token}") String reviewToken,
            @Value("${blog.services.article-service-connect-timeout:3s}") Duration connectTimeout,
            @Value("${blog.services.article-service-read-timeout:5s}") Duration readTimeout
    ) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build()
        );
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.reviewToken = reviewToken;
    }

    @Override
    public void approveArticle(String articleId, String reviewTicketId) {
        approveArticle(articleId, reviewTicketId, null);
    }

    @Override
    public void approveArticle(String articleId, String reviewTicketId, String reviewRequestId) {
        callback("/internal/articles/{articleId}/review-approved", articleId, reviewTicketId, reviewRequestId);
    }

    @Override
    public void rejectArticle(String articleId, String reviewTicketId) {
        rejectArticle(articleId, reviewTicketId, null);
    }

    @Override
    public void rejectArticle(String articleId, String reviewTicketId, String reviewRequestId) {
        callback("/internal/articles/{articleId}/review-rejected", articleId, reviewTicketId, reviewRequestId);
    }

    private void callback(String path, String articleId, String reviewTicketId, String reviewRequestId) {
        if (reviewRequestId == null) {
            restClient.post()
                    .uri(path, articleId)
                    .header("X-Internal-Token", reviewToken)
                    .header("X-Review-Ticket-Id", reviewTicketId)
                    .retrieve()
                    .toBodilessEntity();
            return;
        }
        restClient.post()
                .uri(path, articleId)
                .header("X-Internal-Token", reviewToken)
                .header("X-Review-Ticket-Id", reviewTicketId)
                .header("X-Review-Request-Id", reviewRequestId)
                .retrieve()
                .toBodilessEntity();
    }
}
