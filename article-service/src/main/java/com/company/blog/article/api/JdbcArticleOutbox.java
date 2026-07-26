package com.company.blog.article.api;

import com.company.blog.article.domain.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
/**
 * 将文章领域事件持久化到 PostgreSQL Outbox 表。
 *
 * <p>发布文章时先写本地事件记录，再由调度器投递给搜索服务。这样即使远程调用暂时失败，
 * 发布操作也不会丢失需要建立的搜索索引。</p>
 */
public class JdbcArticleOutbox implements ArticleOutbox {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    public JdbcArticleOutbox(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void appendArticleEvents(List<DomainEvent> events) {
        append(events, null);
    }

    @Override
    public void appendArticleEvents(StoredArticle article, List<DomainEvent> events) {
        append(events, article);
    }

    @Override
    public void appendAuthorNotification(
            StoredArticle article,
            String eventType,
            String title,
            String content
    ) {
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("recipientUserId", article.article().authorId());
            payload.put("type", eventType);
            payload.put("title", title);
            payload.put("content", content);
            payload.put("resourceType", "ARTICLE");
            payload.put("resourceId", article.article().id());
            insert(article.article().id(), eventType, OBJECT_MAPPER.writeValueAsString(payload));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize notification event", ex);
        }
    }

    private void append(List<DomainEvent> events, StoredArticle article) {
        for (DomainEvent event : events) {
            insert(event.aggregateId(), event.type(), payloadJson(event, article));
        }
    }

    private void insert(String aggregateId, String eventType, String payloadJson) {
        jdbcTemplate.update(
                """
                        insert into domain_event
                            (id, aggregate_type, aggregate_id, event_type, payload_json, status, retry_count)
                        values (?, ?, ?, ?, ?, 'PENDING', 0)
                        """,
                UUID.randomUUID().toString(),
                "article",
                aggregateId,
                eventType,
                payloadJson
        );
    }

    private static String payloadJson(DomainEvent event, StoredArticle storedArticle) {
        try {
            // 发布事件携带完整索引快照，调度器无需再次回查文章仓储。
            if (storedArticle == null || !"ArticlePublished".equals(event.type())) {
                return OBJECT_MAPPER.writeValueAsString(Map.of(
                        "articleId", event.aggregateId(),
                        "type", event.type(),
                        "occurredAt", event.occurredAt().toString()
                ));
            }
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("articleId", event.aggregateId());
            payload.put("title", storedArticle.article().title());
            payload.put("summary", summary(storedArticle.content().plainText()));
            payload.put("plainText", storedArticle.content().plainText());
            payload.put("tags", storedArticle.tagIds());
            payload.put("categoryId", storedArticle.categoryId());
            payload.put("authorId", storedArticle.article().authorId());
            payload.put("authorName", storedArticle.article().authorId());
            payload.put("visibilityType", storedArticle.article().visibilityType().name());
            payload.put("targetOrgIds", storedArticle.article().visibilityTargetIds());
            payload.put("status", storedArticle.article().status().name());
            payload.put("publishedAt", event.occurredAt().toString());
            payload.put("updatedAt", storedArticle.article().updatedAt().toString());
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize domain event", ex);
        }
    }

    private static String summary(String plainText) {
        if (plainText == null) {
            return "";
        }
        return plainText.length() <= 280 ? plainText : plainText.substring(0, 280);
    }
}
