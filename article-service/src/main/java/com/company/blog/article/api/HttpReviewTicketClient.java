package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpReviewTicketClient implements ReviewTicketClient {
    private final RestClient restClient;
    private final String reviewToken;

    public HttpReviewTicketClient(
            @Value("${blog.services.review-service-url:http://review-service:8086}") String baseUrl,
            @Value("${blog.internal.review-token:local-review-token}") String reviewToken
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.reviewToken = reviewToken;
    }

    @Override
    public void createTicket(Article article, SubmitPublishRequest request) {
        restClient.post()
                .uri("/internal/reviews/tickets")
                .header("X-Internal-Token", reviewToken)
                .body(new CreateReviewTicketRequest(
                        article.id(),
                        article.reviewRequestId(),
                        article.authorId(),
                        request.visibilityType(),
                        request.targetOrgIds()
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
