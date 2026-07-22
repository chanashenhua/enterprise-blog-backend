package com.company.blog.review.api;

public interface ArticlePublishCallbackClient {
    void approveArticle(String articleId, String reviewTicketId);

    default void approveArticle(String articleId, String reviewTicketId, String reviewRequestId) {
        approveArticle(articleId, reviewTicketId);
    }

    default void rejectArticle(String articleId, String reviewTicketId) {
        throw new UnsupportedOperationException("Article rejection callback is not configured");
    }

    default void rejectArticle(String articleId, String reviewTicketId, String reviewRequestId) {
        rejectArticle(articleId, reviewTicketId);
    }
}
