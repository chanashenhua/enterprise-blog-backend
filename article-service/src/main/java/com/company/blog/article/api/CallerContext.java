package com.company.blog.article.api;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;

/**
 * 网关注入到下游服务的调用者身份快照。
 *
 * <p>文章服务将它原样传给权限服务，而不是在本服务复制一套角色和组织授权规则。</p>
 */
public record CallerContext(String userId, Set<String> roles, Set<String> departmentIds, Set<String> teamIds) {
    public CallerContext {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        departmentIds = departmentIds == null ? Set.of() : Set.copyOf(departmentIds);
        teamIds = teamIds == null ? Set.of() : Set.copyOf(teamIds);
    }

    static CallerContext from(HttpHeaders headers) {
        return new CallerContext(
                headers.getFirst("X-User-Id"),
                commaSeparated(headers.getFirst("X-User-Roles")),
                commaSeparated(headers.getFirst("X-Department-Ids")),
                commaSeparated(headers.getFirst("X-Team-Ids"))
        );
    }

    private static Set<String> commaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
