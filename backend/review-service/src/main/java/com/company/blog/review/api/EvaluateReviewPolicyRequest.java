package com.company.blog.review.api;

import java.util.Set;

public record EvaluateReviewPolicyRequest(String visibilityType, Set<String> targetOrgIds) {
    public EvaluateReviewPolicyRequest {
        targetOrgIds = targetOrgIds == null ? Set.of() : Set.copyOf(targetOrgIds);
    }
}