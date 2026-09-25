package com.company.blog.common.security;

import java.util.Locale;
import java.util.Set;

/** 发布请求及可选组织共用的规则；组织是否存在仍由组织服务实时校验。 */
public final class ArticlePublishScope {
    public static final int MAX_TARGETS = 50;
    private ArticlePublishScope() {}

    public static boolean canWrite(Set<String> roles) {
        return roles.contains("AUTHOR") || roles.contains("ADMIN");
    }

    public static String normalize(String type) {
        return type == null ? "" : type.toUpperCase(Locale.ROOT);
    }

    public static String validationError(String type, Set<String> ids) {
        String normalized = normalize(type);
        if (!Set.of("COMPANY", "DEPARTMENT", "TEAM").contains(normalized)) return "UNSUPPORTED_VISIBILITY";
        if (ids == null) return "INVALID_ORG_TARGETS";
        if (normalized.equals("COMPANY")) return ids.isEmpty() ? null : "COMPANY_TARGETS_NOT_EMPTY";
        if (ids.isEmpty() || ids.size() > MAX_TARGETS) return "INVALID_ORG_TARGET_COUNT";
        if (ids.stream().anyMatch(id -> id == null || id.isBlank() || id.length() > 64 || !id.equals(id.trim()))) return "INVALID_ORG_ID";
        return null;
    }

    public static boolean allowsTarget(String type, String id, Set<String> roles, Set<String> departments, Set<String> teams) {
        if (roles.contains("ADMIN")) return true;
        return switch (normalize(type)) {
            case "DEPARTMENT" -> departments.contains(id);
            case "TEAM" -> teams.contains(id);
            default -> false;
        };
    }
}
