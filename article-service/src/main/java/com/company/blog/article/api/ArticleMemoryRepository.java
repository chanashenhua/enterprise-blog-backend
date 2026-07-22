package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class ArticleMemoryRepository {
    private final ConcurrentMap<String, StoredArticle> articles = new ConcurrentHashMap<>();

    public void save(StoredArticle article) {
        articles.put(article.article().id(), article);
    }

    public Optional<StoredArticle> findById(String articleId) {
        return Optional.ofNullable(articles.get(articleId));
    }

    public record StoredArticle(Article article, String contentJson, ArticleContentProjection content, Set<String> tagIds) {
        public StoredArticle {
            tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
        }
    }
}