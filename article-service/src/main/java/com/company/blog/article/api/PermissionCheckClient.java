package com.company.blog.article.api;

import com.company.blog.article.domain.Article;

public interface PermissionCheckClient {
    void requirePublishAllowed(CallerContext callerContext, Article article, SubmitPublishRequest request);

    default void requireReadAllowed(CallerContext callerContext, Article article) {
    }

    default void requireEditAllowed(CallerContext callerContext, Article article) {
    }

    default void requireWithdrawAllowed(CallerContext callerContext, Article article) {
        requireEditAllowed(callerContext, article);
    }

    default void requireDeleteAllowed(CallerContext callerContext, Article article) {
        requireEditAllowed(callerContext, article);
    }
}
