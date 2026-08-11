package com.company.blog.article.api;

public record ArticleEngagement(
        String articleId,
        long viewCount,
        long likeCount,
        long favoriteCount
) {
    public static ArticleEngagement empty(String articleId) {
        return new ArticleEngagement(articleId, 0, 0, 0);
    }
}
