package com.company.blog.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.review.api.EvaluateReviewPolicyRequest;
import com.company.blog.review.api.EvaluateReviewPolicyResponse;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReviewPolicyTest {
    @Test
    void teamVisibilityRequiresReviewForConfiguredTeam() {
        ReviewPolicy policy = new ReviewPolicy(Set.of("t-search"));
        EvaluateReviewPolicyResponse response = policy.evaluate(
                new EvaluateReviewPolicyRequest("team", Set.of("t-search"))
        );

        assertThat(response.reviewRequired()).isTrue();
        assertThat(response.reviewerRole()).isEqualTo("REVIEWER");
    }

    @Test
    void teamVisibilityDoesNotRequireReviewForUnconfiguredTeam() {
        ReviewPolicy policy = new ReviewPolicy(Set.of("t-search"));
        EvaluateReviewPolicyResponse response = policy.evaluate(
                new EvaluateReviewPolicyRequest("team", Set.of("t-pay"))
        );

        assertThat(response.reviewRequired()).isFalse();
        assertThat(response.reviewerRole()).isNull();
    }
}