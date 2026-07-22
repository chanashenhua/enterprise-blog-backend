package com.company.blog.search.service;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.index.ArticleSearchDocument;
import java.util.Locale;

/**
 * 根据文章可见范围筛选搜索候选结果。
 *
 * <p>这是减少无权限候选的本地过滤规则，不替代权限服务的最终判断。</p>
 */
public class SearchVisibilityFilter {
    public boolean isVisible(UserContext user, ArticleSearchDocument document) {
        if (user == null || user.userId() == null || user.userId().isBlank() || !"PUBLISHED".equals(document.status())) {
            return false;
        }
        return switch (normalize(document.visibilityType())) {
            case "company" -> true;
            case "department" -> document.targetOrgIds().stream().anyMatch(user.departmentIds()::contains);
            case "team" -> document.targetOrgIds().stream().anyMatch(user.teamIds()::contains);
            default -> false;
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
