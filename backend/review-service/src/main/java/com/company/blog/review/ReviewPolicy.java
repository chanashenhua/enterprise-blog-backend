package com.company.blog.review;

import com.company.blog.review.api.EvaluateReviewPolicyRequest;
import com.company.blog.review.api.EvaluateReviewPolicyResponse;
import java.util.Set;

public class ReviewPolicy {
    private final Set<String> reviewRequiredTeamIds;

    public ReviewPolicy(Set<String> reviewRequiredTeamIds) {
        this.reviewRequiredTeamIds = reviewRequiredTeamIds == null ? Set.of() : Set.copyOf(reviewRequiredTeamIds);
    }

    public EvaluateReviewPolicyResponse evaluate(EvaluateReviewPolicyRequest request) {
        boolean reviewRequired = "team".equalsIgnoreCase(request.visibilityType())
                && request.targetOrgIds().stream().anyMatch(reviewRequiredTeamIds::contains);
        return new EvaluateReviewPolicyResponse(reviewRequired, reviewRequired ? "REVIEWER" : null);
    }
}