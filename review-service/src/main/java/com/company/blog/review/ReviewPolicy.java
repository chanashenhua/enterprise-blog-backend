package com.company.blog.review;

import com.company.blog.review.api.EvaluateReviewPolicyRequest;
import com.company.blog.review.api.EvaluateReviewPolicyResponse;
import java.util.Set;

/**
 * 审核策略的纯业务规则。
 *
 * <p>当前规则只要求配置名单中的团队文章审核。将规则集中在这里，后续可替换为组织策略表或
 * 规则引擎，而不需要修改文章服务的发布流程。</p>
 */
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
