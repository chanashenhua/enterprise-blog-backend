package com.company.blog.review.api;

public interface ReviewAuditClient {
    void send(String payloadJson);
}
