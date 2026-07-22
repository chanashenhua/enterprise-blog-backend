package com.company.blog.article.api;

import com.company.blog.article.domain.Article;

public interface PermissionCheckClient {
    void requirePublishAllowed(CallerContext callerContext, Article article, SubmitPublishRequest request);
}