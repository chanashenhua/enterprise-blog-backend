package com.company.blog.article.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpReviewPolicyClient implements ReviewPolicyClient {
    private final RestClient restClient;
    private final String reviewToken;

    public HttpReviewPolicyClient(
            @Value("${blog.services.review-service-url:http://review-service:8086}") String baseUrl,
            @Value("${blog.internal.review-token:local-review-token}") String reviewToken
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.reviewToken = reviewToken;
    }

    @Override
    public boolean reviewRequired(SubmitPublishRequest request) {
        EvaluateReviewPolicyResponse response = restClient.post()
                .uri("/internal/reviews/policies/evaluate")
                .header("X-Internal-Token", reviewToken)
                .body(new EvaluateReviewPolicyRequest(request.visibilityType(), request.targetOrgIds()))
                .retrieve()
                .body(EvaluateReviewPolicyResponse.class);
        return response != null && response.reviewRequired();
    }
}
