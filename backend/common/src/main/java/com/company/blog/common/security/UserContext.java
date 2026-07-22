package com.company.blog.common.security;

import java.util.Set;

public record UserContext(
        String userId,
        Set<String> roles,
        Set<String> departmentIds,
        Set<String> teamIds
) {
    public UserContext {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        departmentIds = departmentIds == null ? Set.of() : Set.copyOf(departmentIds);
        teamIds = teamIds == null ? Set.of() : Set.copyOf(teamIds);
    }
}