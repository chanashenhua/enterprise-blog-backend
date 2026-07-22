package com.company.blog.search.service;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.index.ArticleSearchDocument;

public interface PermissionCheckClient {
    boolean canRead(UserContext user, ArticleSearchDocument document);
}
