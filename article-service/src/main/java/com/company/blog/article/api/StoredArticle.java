package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import java.util.Objects;
import java.util.Set;

/**
 * 文章聚合的持久化快照，包含文章状态、原始正文、展示投影和标签关系。
 */
public record StoredArticle(
        Article article,
        String contentJson,
        ArticleContentProjection content,
        Set<String> tagIds
) {
    public StoredArticle {
        Objects.requireNonNull(article, "article must not be null");
        Objects.requireNonNull(contentJson, "contentJson must not be null");
        Objects.requireNonNull(content, "content must not be null");
        tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
    }
}
