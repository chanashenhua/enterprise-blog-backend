package com.company.blog.article.api;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ArticleSearchOutboxDispatcher {
    private final JdbcTemplate jdbcTemplate;
    private final ArticleSearchIndexClient searchIndexClient;
    private final int batchSize;
    private final int maxRetries;

    public ArticleSearchOutboxDispatcher(
            JdbcTemplate jdbcTemplate,
            ArticleSearchIndexClient searchIndexClient,
            @Value("${blog.outbox.search.batch-size:20}") int batchSize,
            @Value("${blog.outbox.search.max-retries:5}") int maxRetries
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.searchIndexClient = searchIndexClient;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${blog.outbox.search.fixed-delay-ms:5000}")
    public void deliverPending() {
        List<Map<String, Object>> events = jdbcTemplate.queryForList(
                "select id, payload_json from domain_event where event_type = 'ArticlePublished' and status = 'PENDING' order by created_at limit ?",
                batchSize
        );
        for (Map<String, Object> event : events) {
            String eventId = (String) event.get("id");
            try {
                searchIndexClient.index((String) event.get("payload_json"));
                jdbcTemplate.update(
                        "update domain_event set status = 'DELIVERED', updated_at = current_timestamp where id = ? and status = 'PENDING'",
                        eventId
                );
            } catch (Exception ex) {
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
    }
}
