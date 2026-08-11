package com.company.blog.comment.api;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CommentNotificationOutboxDispatcher {
    private final JdbcTemplate jdbcTemplate;
    private final CommentNotificationClient notificationClient;
    private final int batchSize;
    private final int maxRetries;

    public CommentNotificationOutboxDispatcher(
            JdbcTemplate jdbcTemplate,
            CommentNotificationClient notificationClient,
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
                        from comment_notification_event
                        where status = 'PENDING'
                        order by created_at
                        limit ?
                        """,
                batchSize
        );
        for (Map<String, Object> event : events) {
            String eventId = (String) event.get("id");
            try {
                notificationClient.send(eventId, (String) event.get("payload_json"));
                jdbcTemplate.update(
                        """
                                update comment_notification_event
                                set status = 'DELIVERED', updated_at = current_timestamp
                                where id = ? and status = 'PENDING'
                                """,
                        eventId
                );
            } catch (Exception ex) {
                jdbcTemplate.update(
                        """
                                update comment_notification_event
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
    }
}
