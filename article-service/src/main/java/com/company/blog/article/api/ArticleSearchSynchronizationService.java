package com.company.blog.article.api;

import com.company.blog.article.domain.ArticleStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleSearchSynchronizationService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ArticleRepository repository;
    private final ArticleSearchIndexClient client;

    public ArticleSearchSynchronizationService(ArticleRepository repository, ArticleSearchIndexClient client) {
        this.repository = repository;
        this.client = client;
    }

    @Transactional
    public void synchronize(String articleId) {
        // Serialize synchronization against article writes; a retried event always reads current state.
        StoredArticle stored = repository.findByIdForUpdate(articleId).orElse(null);
        if (stored == null) {
            client.delete(articleId, Long.MAX_VALUE);
        } else if (stored.article().status() != ArticleStatus.PUBLISHED) {
            client.delete(articleId, stored.revision());
        } else {
            client.index(payload(stored));
        }
    }

    static String payload(StoredArticle stored) {
        Map<String, Object> data = new LinkedHashMap<>();
        String plain = stored.content().plainText();
        data.put("articleId", stored.article().id());
        data.put("title", stored.article().title());
        data.put("summary", plain.length() <= 280 ? plain : plain.substring(0, 280));
        data.put("plainText", plain);
        data.put("tags", stored.tagIds());
        data.put("categoryId", stored.categoryId());
        data.put("authorId", stored.article().authorId());
        data.put("authorName", stored.article().authorId());
        data.put("visibilityType", stored.article().visibilityType().name());
        data.put("targetOrgIds", stored.article().visibilityTargetIds());
        data.put("status", "PUBLISHED");
        data.put("publishedAt", stored.article().updatedAt().toString());
        data.put("updatedAt", stored.article().updatedAt().toString());
        data.put("revision", stored.revision());
        try { return MAPPER.writeValueAsString(data); }
        catch (Exception exception) { throw new IllegalStateException("Cannot serialize search projection", exception); }
    }
}
