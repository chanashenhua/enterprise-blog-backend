package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public interface PermissionCheckClient {
    void requirePublishAllowed(CallerContext callerContext, Article article, SubmitPublishRequest request);

    default void requireReadAllowed(CallerContext callerContext, Article article) {
    }

    default boolean readAllowed(CallerContext callerContext, Article article) {
        try {
            requireReadAllowed(callerContext, article);
            return true;
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.FORBIDDEN) {
                return false;
            }
            throw exception;
        }
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
