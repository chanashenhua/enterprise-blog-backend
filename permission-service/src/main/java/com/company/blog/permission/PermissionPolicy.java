package com.company.blog.permission;

import com.company.blog.permission.api.PermissionCheckRequest;
import java.util.Set;

/**
 * 集中定义文章相关的授权规则。
 *
 * <p>发布、审核和编辑通过角色或所有权判断；阅读再根据公司、部门、团队的目标组织范围判断。
 * 新的权限动作应在这里显式加入，默认拒绝未知动作。</p>
 */
public class PermissionPolicy {
    public PermissionDecision check(PermissionCheckRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            return PermissionDecision.deny("UNAUTHENTICATED");
        }

        return switch (request.action()) {
            case "article.read" -> checkArticleRead(request);
            case "article.publish" -> hasAnyRole(request.roles(), "AUTHOR", "ADMIN")
                    ? PermissionDecision.allow()
                    : PermissionDecision.deny("ROLE_NOT_ALLOWED");
            case "article.review" -> hasAnyRole(request.roles(), "REVIEWER", "ADMIN")
                    ? PermissionDecision.allow()
                    : PermissionDecision.deny("ROLE_NOT_ALLOWED");
            case "article.edit", "article.withdraw", "article.delete" -> canEditArticle(request)
                    ? PermissionDecision.allow()
                    : PermissionDecision.deny("NOT_RESOURCE_OWNER");
            default -> PermissionDecision.deny("UNSUPPORTED_ACTION");
        };
    }

    private PermissionDecision checkArticleRead(PermissionCheckRequest request) {
        // 文章服务传入的可见范围决定需要比对哪一种组织集合。
        return switch (request.visibilityType()) {
            case "company" -> PermissionDecision.allow();
            case "department" -> intersects(request.departmentIds(), request.targetOrgIds())
                    ? PermissionDecision.allow()
                    : PermissionDecision.deny("USER_OUTSIDE_TARGET_ORG");
            case "team" -> intersects(request.teamIds(), request.targetOrgIds())
                    ? PermissionDecision.allow()
                    : PermissionDecision.deny("USER_OUTSIDE_TARGET_ORG");
            default -> PermissionDecision.deny("UNSUPPORTED_VISIBILITY");
        };
    }

    private static boolean canEditArticle(PermissionCheckRequest request) {
        return request.userId().equals(request.resourceOwnerId()) || request.roles().contains("ADMIN");
    }

    private static boolean hasAnyRole(Set<String> roles, String firstRole, String secondRole) {
        return roles.contains(firstRole) || roles.contains(secondRole);
    }

    private static boolean intersects(Set<String> left, Set<String> right) {
        return left.stream().anyMatch(right::contains);
    }
}
