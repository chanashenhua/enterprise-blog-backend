package com.company.blog.stats.api;

import java.util.List;

public record AdminInteractionOverview(
        long viewCount,
        long likeCount,
        long favoriteCount,
        long activeArticleCount,
        long engagedUserCount,
        List<ArticleInteractionRanking> topArticles
) {
    public AdminInteractionOverview {
        topArticles = topArticles == null ? List.of() : List.copyOf(topArticles);
    }
}
