package com.company.blog.review.api;

import org.springframework.http.HttpHeaders;

public interface ReviewPermissionClient {
    void requireReviewAllowed(HttpHeaders headers);
}