package com.company.blog.search.service;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.api.SearchArticleRequest;
import com.company.blog.search.api.SearchArticleResponse;
import com.company.blog.search.index.ArticleSearchDocument;

public interface ArticleSearchRepository {
    void index(ArticleSearchDocument document);

    void delete(String articleId);

    SearchArticleResponse search(UserContext user, SearchArticleRequest request);
}
