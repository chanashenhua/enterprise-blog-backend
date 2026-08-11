package com.company.blog.article.api;

public record ContentOperationsOverview(
        long totalArticleCount,
        long publishedArticleCount,
        long draftArticleCount,
        long pendingReviewArticleCount,
        long withdrawnArticleCount,
        long categorizedPublishedCount,
        long taggedPublishedCount,
        long collectionCount,
        long collectionArticleCount,
        long collectionOwnerCount
) {
}
