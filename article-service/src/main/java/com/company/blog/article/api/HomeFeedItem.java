package com.company.blog.article.api;

import java.time.Instant;
import java.util.Set;

public record HomeFeedItem(
        String articleId,
        String authorId,
        String title,
        String summary,
        Set<String> tagIds,
        String categoryId,
        Instant publishedAt,
        long viewCount,
        long likeCount,
        long favoriteCount
) {
    public HomeFeedItem {
        tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
    }
}
