package com.company.blog.search.service;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.api.SearchArticleRequest;
import com.company.blog.search.api.SearchArticleResponse;
import com.company.blog.search.index.ArticleSearchDocument;
import org.springframework.stereotype.Service;

@Service
/**
 * 搜索用例的薄编排层。
 *
 * <p>索引的存取细节留在仓储实现中；可见范围过滤和权限服务校验保持独立，以便 Elasticsearch
 * 只是候选集来源，而不是授权事实来源。</p>
 */
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
        // 两层判断缺一不可：组织范围先过滤，权限服务再给出最终授权结论。
        return visibilityFilter.isVisible(user, document) && permissionCheckClient.canRead(user, document);
    }
}
