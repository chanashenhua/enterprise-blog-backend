package com.company.blog.article.api;

import java.time.Instant;
import java.util.Set;

/**
 * 一次不可变的文章内容快照，用于追溯草稿修改历史。
 */
public record ArticleContentVersion(
        String articleId,
        int versionNo,
        String title,
        String contentJson,
        String renderedHtml,
        String plainText,
        Set<String> tagIds,
        String categoryId,
        String createdBy,
        Instant createdAt
) {
    public ArticleContentVersion {
        tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
        categoryId = categoryId == null || categoryId.isBlank() ? null : categoryId;
    }
}
