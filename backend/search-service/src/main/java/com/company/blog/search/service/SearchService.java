package com.company.blog.search.service;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.api.SearchArticleRequest;
import com.company.blog.search.api.SearchArticleResponse;
import com.company.blog.search.index.ArticleSearchDocument;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
    private final ArticleSearchRepository repository;
    private final PermissionCheckClient permissionCheckClient;
    private final SearchVisibilityFilter visibilityFilter = new SearchVisibilityFilter();

    public SearchService(ArticleSearchRepository repository, PermissionCheckClient permissionCheckClient) {
        this.repository = repository;
        this.permissionCheckClient = permissionCheckClient;
    }

    public void index(ArticleSearchDocument document) {
        repository.index(document);
    }

    public void delete(String articleId) {
        repository.delete(articleId);
    }

    public SearchArticleResponse search(UserContext user, SearchArticleRequest request) {
        return repository.search(user, request);
    }

    public boolean visibleTo(UserContext user, ArticleSearchDocument document) {
        return visibilityFilter.isVisible(user, document) && permissionCheckClient.canRead(user, document);
    }
}
