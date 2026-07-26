package com.company.blog.search.api;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record SearchArticleResponse(List<Article> items, long total, int page, int size) {
    public SearchArticleResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Article(
            String articleId,
            String title,
            String summary,
            Set<String> tags,
            String categoryId,
            String authorId,
            String authorName,
            Instant publishedAt,
            Instant updatedAt
    ) {
        public Article {
            tags = tags == null ? Set.of() : Set.copyOf(tags);
        }
    }
}
