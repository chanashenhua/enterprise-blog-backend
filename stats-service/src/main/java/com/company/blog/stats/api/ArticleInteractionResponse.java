package com.company.blog.stats.api;

public record ArticleInteractionResponse(
        String articleId,
        long viewCount,
        long likeCount,
        long favoriteCount,
        boolean liked,
        boolean favorited
) {
    static ArticleInteractionResponse from(String articleId, InteractionSnapshot snapshot) {
        return new ArticleInteractionResponse(
                articleId,
                snapshot.viewCount(),
                snapshot.likeCount(),
                snapshot.favoriteCount(),
                snapshot.liked(),
                snapshot.favorited()
        );
    }
}
