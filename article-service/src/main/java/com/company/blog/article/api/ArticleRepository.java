package com.company.blog.article.api;

import java.util.List;
import java.util.Optional;

/**
 * 文章聚合的持久化边界。
 *
 * <p>应用服务只依赖这个接口，不关心数据来自内存还是 PostgreSQL。写操作需要保存文章主表、
 * 正文、标签和可见范围；状态转换时使用带行锁的查询，避免并发审核回调相互覆盖。</p>
 */
public interface ArticleRepository {
    void save(StoredArticle article);

    Optional<StoredArticle> findById(String articleId);

    Optional<StoredArticle> findByIdForUpdate(String articleId);

    List<StoredArticle> findByAuthorId(String authorId);

    List<StoredArticle> findPublished(int limit);

    List<StoredArticle> findPublishedByCategory(String categoryId, int limit);

    List<StoredArticle> findPublishedByTag(String tagId, int limit);

    ArticleContentVersion appendContentVersion(StoredArticle article, String createdBy);

    List<ArticleContentVersion> findContentVersions(String articleId);
}
