package com.company.blog.article.api;

import com.company.blog.article.domain.ArticleStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 仅供不启动数据库的单元测试使用的文章聚合存储。
 *
 * <p>Spring 运行时不再注册这个实现，正式服务统一使用 {@link JdbcArticleRepository}。</p>
 */
public class ArticleMemoryRepository implements ArticleRepository {
    private final ConcurrentMap<String, StoredArticle> articles = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<ArticleContentVersion>> versions =
            new ConcurrentHashMap<>();

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

    @Override
    public List<StoredArticle> findByAuthorId(String authorId) {
        return articles.values().stream()
                .filter(article -> article.article().authorId().equals(authorId))
                .filter(article -> article.article().status() != ArticleStatus.DELETED)
                .sorted(Comparator.comparing(
                        (StoredArticle article) -> article.article().updatedAt()
                ).reversed())
                .toList();
    }

    @Override
    public List<StoredArticle> findPublished(int limit) {
        return publishedArticles()
                .limit(limit)
                .toList();
    }

    @Override
    public List<StoredArticle> findPublishedByCategory(String categoryId, int limit) {
        return publishedArticles()
                .filter(article -> categoryId.equals(article.categoryId()))
                .limit(limit)
                .toList();
    }

    @Override
    public List<StoredArticle> findPublishedByTag(String tagId, int limit) {
        return publishedArticles()
                .filter(article -> article.tagIds().contains(tagId))
                .limit(limit)
                .toList();
    }

    private java.util.stream.Stream<StoredArticle> publishedArticles() {
        return articles.values().stream()
                .filter(article -> article.article().status() == ArticleStatus.PUBLISHED)
                .sorted(Comparator.comparing(
                        (StoredArticle article) -> article.article().updatedAt()
                ).reversed().thenComparing(article -> article.article().id()));
    }

    @Override
    public ArticleContentVersion appendContentVersion(StoredArticle storedArticle, String createdBy) {
        CopyOnWriteArrayList<ArticleContentVersion> articleVersions = versions.computeIfAbsent(
                storedArticle.article().id(),
                ignored -> new CopyOnWriteArrayList<>()
        );
        ArticleContentVersion version = new ArticleContentVersion(
                storedArticle.article().id(),
                articleVersions.size() + 1,
                storedArticle.article().title(),
                storedArticle.contentJson(),
                storedArticle.content().renderedHtml(),
                storedArticle.content().plainText(),
                storedArticle.tagIds(),
                storedArticle.categoryId(),
                createdBy,
                Instant.now()
        );
        articleVersions.add(version);
        return version;
    }

    @Override
    public List<ArticleContentVersion> findContentVersions(String articleId) {
        return List.copyOf(versions.getOrDefault(articleId, new CopyOnWriteArrayList<>()));
    }
}
