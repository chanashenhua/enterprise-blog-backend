package com.company.blog.stats.api;

public record ArticleInteractionRanking(
        String articleId,
        long viewCount,
        long likeCount,
        long favoriteCount
) {
    public long engagementCount() {
        return likeCount + favoriteCount;
    }
}
