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
    public void appendArticleEvents(ArticleMemoryRepository.StoredArticle article, List<DomainEvent> events) {
        append(events, article);
    }

    private void append(List<DomainEvent> events, ArticleMemoryRepository.StoredArticle article) {
        for (DomainEvent event : events) {
            jdbcTemplate.update(
                    """
                            insert into domain_event
                                (id, aggregate_type, aggregate_id, event_type, payload_json, status, retry_count)
                            values (?, ?, ?, ?, ?, 'PENDING', 0)
                            """,
                    UUID.randomUUID().toString(),
                    "article",
                    event.aggregateId(),
                    event.type(),
                    payloadJson(event, article)
            );
        }
    }

    private static String payloadJson(DomainEvent event, ArticleMemoryRepository.StoredArticle storedArticle) {
        try {
            // 发布事件携带完整索引快照，调度器无需回查仍是内存实现的文章服务。
            if (storedArticle == null || !"ArticlePublished".equals(event.type())) {
                return OBJECT_MAPPER.writeValueAsString(Map.of(
                        "articleId", event.aggregateId(),
                        "type", event.type(),
                        "occurredAt", event.occurredAt().toString()
                ));
            }
            return OBJECT_MAPPER.writeValueAsString(Map.ofEntries(
                    Map.entry("articleId", event.aggregateId()),
                    Map.entry("title", storedArticle.article().title()),
                    Map.entry("summary", summary(storedArticle.content().plainText())),
                    Map.entry("plainText", storedArticle.content().plainText()),
                    Map.entry("tags", storedArticle.tagIds()),
                    Map.entry("authorId", storedArticle.article().authorId()),
                    Map.entry("authorName", storedArticle.article().authorId()),
                    Map.entry("visibilityType", storedArticle.article().visibilityType().name()),
                    Map.entry("targetOrgIds", storedArticle.article().visibilityTargetIds()),
                    Map.entry("status", storedArticle.article().status().name()),
                    Map.entry("publishedAt", event.occurredAt().toString()),
                    Map.entry("updatedAt", storedArticle.article().updatedAt().toString())
            ));
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
