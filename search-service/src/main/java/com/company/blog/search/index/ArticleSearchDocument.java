package com.company.blog.search.index;

import com.company.blog.search.api.IndexArticleRequest;
import java.time.Instant;
import java.util.Set;

public record ArticleSearchDocument(
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
    public ArticleSearchDocument {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        targetOrgIds = targetOrgIds == null ? Set.of() : Set.copyOf(targetOrgIds);
    }

    public static ArticleSearchDocument from(IndexArticleRequest request) {
        return new ArticleSearchDocument(
                request.articleId(), request.title(), request.summary(), request.plainText(), request.tags(),
                request.categoryId(), request.authorId(), request.authorName(), request.visibilityType(), request.targetOrgIds(),
                request.status(), request.publishedAt(), request.updatedAt()
        );
    }

    public ArticleSearchDocument(
            String articleId,
            String title,
            String summary,
            String plainText,
            Set<String> tags,
            String authorId,
            String authorName,
            String visibilityType,
            Set<String> targetOrgIds,
            String status,
            Instant publishedAt,
            Instant updatedAt
    ) {
        this(
                articleId, title, summary, plainText, tags, null, authorId, authorName,
                visibilityType, targetOrgIds, status, publishedAt, updatedAt
        );
    }
}
