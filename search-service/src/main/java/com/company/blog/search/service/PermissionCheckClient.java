package com.company.blog.search.service;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.index.ArticleSearchDocument;

public interface PermissionCheckClient {
    boolean canRead(UserContext user, ArticleSearchDocument document);

    default java.util.Set<String> readableIds(UserContext user, java.util.List<ArticleSearchDocument> documents) {
        return documents.stream().filter(document -> canRead(user, document))
                .map(ArticleSearchDocument::articleId).collect(java.util.stream.Collectors.toSet());
    }
}
