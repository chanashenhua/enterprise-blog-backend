package com.company.blog.review.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpReviewAuditClient implements ReviewAuditClient {
    private final RestClient restClient;
    private final String internalToken;

    public HttpReviewAuditClient(
            @Value("${blog.services.audit-service-url:http://audit-service:8092}") String baseUrl,
            @Value("${blog.internal.audit-token}") String internalToken
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.internalToken = internalToken;
    }

    @Override
    public void send(String payloadJson) {
        restClient.post()
                .uri("/internal/audits")
                .header("X-Internal-Token", internalToken)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(payloadJson)
                .retrieve()
                .toBodilessEntity();
    }
}
