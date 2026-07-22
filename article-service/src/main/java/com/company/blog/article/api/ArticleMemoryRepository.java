package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
/**
 * MVP 阶段的文章聚合存储实现。
 *
 * <p>它用并发 Map 保存文章、编辑器 JSON 和检索投影，方便先验证业务链路；服务重启后
 * 数据会丢失，因此生产化时应替换为 PostgreSQL 实现，调用方不应依赖其内存特性。</p>
 */
public class ArticleMemoryRepository {
    private final ConcurrentMap<String, StoredArticle> articles = new ConcurrentHashMap<>();

    public void save(StoredArticle article) {
        articles.put(article.article().id(), article);
    }

    public Optional<StoredArticle> findById(String articleId) {
        return Optional.ofNullable(articles.get(articleId));
    }

    /** 文章聚合连同用于展示和索引的派生内容一起保存，避免重复解析编辑器 JSON。 */
    public record StoredArticle(Article article, String contentJson, ArticleContentProjection content, Set<String> tagIds) {
        public StoredArticle {
            tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
        }
    }
}
