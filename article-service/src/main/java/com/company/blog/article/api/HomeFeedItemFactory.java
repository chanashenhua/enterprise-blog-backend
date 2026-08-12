package com.company.blog.article.api;

final class HomeFeedItemFactory {
    private static final int SUMMARY_LENGTH = 150;

    private HomeFeedItemFactory() {
    }

    static HomeFeedItem from(StoredArticle storedArticle, ArticleEngagement engagement) {
        ArticleEngagement metrics = engagement == null
                ? ArticleEngagement.empty(storedArticle.article().id())
                : engagement;
        return new HomeFeedItem(
                storedArticle.article().id(),
                storedArticle.article().authorId(),
                storedArticle.article().title(),
                summary(storedArticle.content().plainText()),
                storedArticle.tagIds(),
                storedArticle.categoryId(),
                storedArticle.article().updatedAt(),
                metrics.viewCount(),
                metrics.likeCount(),
                metrics.favoriteCount()
        );
    }

    private static String summary(String plainText) {
        if (plainText == null || plainText.isBlank()) return "暂无摘要";
        String normalized = plainText.trim().replaceAll("\\s+", " ");
        return normalized.length() <= SUMMARY_LENGTH
                ? normalized
                : normalized.substring(0, SUMMARY_LENGTH).stripTrailing() + "…";
    }
}
