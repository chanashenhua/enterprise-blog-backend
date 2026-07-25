package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import java.util.Set;

public record ArticleResponse(
        String id,
        String authorId,
        String title,
        String status,
        String visibilityType,
        Set<String> visibilityTargetIds,
        Set<String> tagIds,
        String categoryId,
        String contentJson,
        String renderedHtml,
        String plainText
) {
    static ArticleResponse from(StoredArticle storedArticle) {
        Article article = storedArticle.article();
        ArticleContentProjection content = storedArticle.content();
        return new ArticleResponse(
                article.id(),
                article.authorId(),
                article.title(),
                article.status().name(),
                article.visibilityType() == null ? null : article.visibilityType().name(),
                article.visibilityTargetIds(),
                storedArticle.tagIds(),
                storedArticle.categoryId(),
                storedArticle.contentJson(),
                content.renderedHtml(),
                content.plainText()
        );
    }
}
