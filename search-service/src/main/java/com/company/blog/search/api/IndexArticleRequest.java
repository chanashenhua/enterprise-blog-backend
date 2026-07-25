package com.company.blog.search.api;

import java.time.Instant;
import java.util.Set;

public record IndexArticleRequest(
        String articleId,
        String title,
        String summary,
        String plainText,
        Set<String> tags,
        String categoryId,
        String authorId,
        String authorName,
        String visibilityType,
        Set<String> targetOrgIds,
        String status,
        Instant publishedAt,
        Instant updatedAt
) {
    public IndexArticleRequest {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        targetOrgIds = targetOrgIds == null ? Set.of() : Set.copyOf(targetOrgIds);
    }
}
