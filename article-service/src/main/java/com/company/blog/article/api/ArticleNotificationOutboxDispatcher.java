package com.company.blog.article.api;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 将审核结果通知事件从文章 Outbox 投递到通知服务。
 */
@Component
public class ArticleNotificationOutboxDispatcher {
    private final JdbcTemplate jdbcTemplate;
    private final ArticleNotificationClient notificationClient;
    private final int batchSize;
    private final int maxRetries;

    public ArticleNotificationOutboxDispatcher(
            JdbcTemplate jdbcTemplate,
            ArticleNotificationClient notificationClient,
            @Value("${blog.outbox.notification.batch-size:20}") int batchSize,
            @Value("${blog.outbox.notification.max-retries:5}") int maxRetries
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationClient = notificationClient;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${blog.outbox.notification.fixed-delay-ms:5000}")
    public void deliverPending() {
        List<Map<String, Object>> events = jdbcTemplate.queryForList(
                """
                        select id, payload_json
                        from domain_event
                        where event_type in ('REVIEW_APPROVED', 'REVIEW_REJECTED')
                          and status = 'PENDING'
                        order by created_at
                        limit ?
                        """,
                batchSize
        );
        for (Map<String, Object> event : events) {
            String eventId = (String) event.get("id");
            try {
                notificationClient.send(eventId, (String) event.get("payload_json"));
                markDelivered(eventId);
            } catch (Exception ex) {
                markFailedAttempt(eventId);
            }
        }
    }

    private void markDelivered(String eventId) {
        jdbcTemplate.update(
                "update domain_event set status = 'DELIVERED', updated_at = current_timestamp where id = ? and status = 'PENDING'",
                eventId
        );
    }

    private void markFailedAttempt(String eventId) {
        jdbcTemplate.update(
                """
                        update domain_event
                        set retry_count = retry_count + 1,
                            status = case when retry_count + 1 >= ? then 'FAILED' else 'PENDING' end,
                            updated_at = current_timestamp
                        where id = ? and status = 'PENDING'
                        """,
                maxRetries,
                eventId
        );
    }
}
