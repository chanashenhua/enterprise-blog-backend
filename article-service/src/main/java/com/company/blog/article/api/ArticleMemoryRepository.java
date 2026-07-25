package com.company.blog.article.api;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 仅供不启动数据库的单元测试使用的文章聚合存储。
 *
 * <p>Spring 运行时不再注册这个实现，正式服务统一使用 {@link JdbcArticleRepository}。</p>
 */
public class ArticleMemoryRepository implements ArticleRepository {
    private final ConcurrentMap<String, StoredArticle> articles = new ConcurrentHashMap<>();

    @Override
    public void save(StoredArticle article) {
        articles.put(article.article().id(), article);
    }

    @Override
    public Optional<StoredArticle> findById(String articleId) {
        return Optional.ofNullable(articles.get(articleId));
    }

    @Override
    public Optional<StoredArticle> findByIdForUpdate(String articleId) {
        return findById(articleId);
    }
}
