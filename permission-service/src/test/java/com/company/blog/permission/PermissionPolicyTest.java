package com.company.blog.permission;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.permission.api.PermissionCheckRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionPolicyTest {
    private final PermissionPolicy policy = new PermissionPolicy();

    @Test
    void deniesTeamArticleWhenUserIsOutsideTargetTeam() {
        PermissionDecision decision = policy.check(request(
                Set.of("READER"),
                Set.of("d-1"),
                Set.of("t-1"),
                "article.read",
                "u-2",
                "team",
                Set.of("t-2")
        ));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("USER_OUTSIDE_TARGET_ORG");
    }

    @Test
    void allowsCompanyArticleReadForAuthenticatedUser() {
        PermissionDecision decision = policy.check(request(
                Set.of("READER"), Set.of(), Set.of(), "article.read", "u-2", "company", Set.of()
        ));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reason()).isEqualTo("ALLOWED");
    }

    @Test
    void allowsDepartmentArticleReadWhenDepartmentIntersectsTarget() {
        PermissionDecision decision = policy.check(request(
                Set.of("READER"), Set.of("d-platform"), Set.of(), "article.read", "u-2", "department", Set.of("d-platform")
        ));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reason()).isEqualTo("ALLOWED");
    }

    @Test
    void allowsPublishForAuthor() {
        PermissionDecision decision = policy.check(request(
                Set.of("AUTHOR"), Set.of(), Set.of(), "article.publish", "u-2", "company", Set.of()
        ));

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void allowsReviewForReviewer() {
        PermissionDecision decision = policy.check(request(
                Set.of("REVIEWER"), Set.of(), Set.of(), "article.review", "u-2", "company", Set.of()
        ));

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void allowsEditForOwner() {
        PermissionDecision decision = policy.check(request(
                Set.of("READER"), Set.of(), Set.of(), "article.edit", "u-1", "company", Set.of()
        ));

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void deniesUnknownAction() {
        PermissionDecision decision = policy.check(request(
                Set.of("READER"), Set.of(), Set.of(), "article.delete", "u-1", "company", Set.of()
        ));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("UNSUPPORTED_ACTION");
    }

    private static PermissionCheckRequest request(
            Set<String> roles,
            Set<String> departmentIds,
            Set<String> teamIds,
            String action,
            String resourceOwnerId,
            String visibilityType,
            Set<String> targetOrgIds
    ) {
        return new PermissionCheckRequest(
                "u-1",
                roles,
                departmentIds,
                teamIds,
                action,
                "article",
                resourceOwnerId,
                visibilityType,
                targetOrgIds
        );
    }
}