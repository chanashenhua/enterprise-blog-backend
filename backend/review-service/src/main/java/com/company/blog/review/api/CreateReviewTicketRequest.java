package com.company.blog.review.api;

import java.util.Set;

public record CreateReviewTicketRequest(
        String articleId,
        String reviewRequestId,
        String authorId,
        String visibilityType,
        Set<String> targetOrgIds
) {
    public CreateReviewTicketRequest {
        requireText(articleId, "articleId");
        requireText(reviewRequestId, "reviewRequestId");
        targetOrgIds = targetOrgIds == null ? Set.of() : Set.copyOf(targetOrgIds);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
