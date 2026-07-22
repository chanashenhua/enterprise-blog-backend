package com.company.blog.article.api;

import java.util.Set;

record PermissionCheckRequest(
        String userId,
        Set<String> roles,
        Set<String> departmentIds,
        Set<String> teamIds,
        String action,
        String resourceType,
        String resourceOwnerId,
        String visibilityType,
        Set<String> targetOrgIds
) {
    PermissionCheckRequest {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        departmentIds = departmentIds == null ? Set.of() : Set.copyOf(departmentIds);
        teamIds = teamIds == null ? Set.of() : Set.copyOf(teamIds);
        targetOrgIds = targetOrgIds == null ? Set.of() : Set.copyOf(targetOrgIds);
    }
}