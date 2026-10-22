package com.company.blog.search.api;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record SearchArticleResponse(List<Article> items, Long total, int page, int size, boolean hasMore, boolean limited) {
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
            Instant updatedAt,
            List<HighlightPart> titleHighlights,
            List<HighlightPart> summaryHighlights
    ) {
        public Article {
            tags = tags == null ? Set.of() : Set.copyOf(tags);
            titleHighlights = List.copyOf(titleHighlights);
            summaryHighlights = List.copyOf(summaryHighlights);
        }
    }

    public record HighlightPart(String text, boolean matched) { }
}
