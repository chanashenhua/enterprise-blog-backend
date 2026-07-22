package com.company.blog.article.api;

import java.util.Set;

record EvaluateReviewPolicyRequest(String visibilityType, Set<String> targetOrgIds) {
    EvaluateReviewPolicyRequest {
        targetOrgIds = targetOrgIds == null ? Set.of() : Set.copyOf(targetOrgIds);
    }
}