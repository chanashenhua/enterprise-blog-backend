package com.company.blog.common.security;

import java.util.Set;

/**
 * 网关认证完成后向下游传播的最小用户上下文。
 *
 * <p>其中的组织集合用于范围筛选，真正的动作授权仍由权限服务判断。</p>
 */
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
