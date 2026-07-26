package com.company.blog.article.api;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
/**
 * 定时投递待发送的文章发布事件到搜索服务。
 *
 * <p>采用至少一次投递：只有搜索服务成功接收后才标记 DELIVERED；失败会累计重试次数，
 * 超过上限后标为 FAILED，留给运维人员或补偿任务处理。</p>
 */
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
    /** 每轮限量拉取，避免大量积压事件占满数据库连接或压垮搜索服务。 */
    public void deliverPending() {
        List<Map<String, Object>> events = jdbcTemplate.queryForList(
                """
                        select id, aggregate_id, event_type, payload_json
                        from domain_event
                        where event_type in ('ArticlePublished', 'ArticleWithdrawn', 'ArticleDeleted')
                          and status = 'PENDING'
                        order by created_at
                        limit ?
                        """,
                batchSize
        );
        for (Map<String, Object> event : events) {
            String eventId = (String) event.get("id");
            try {
                // 先完成远程写入，再改变本地状态，保证失败时仍可被下一轮重新投递。
                if ("ArticlePublished".equals(event.get("event_type"))) {
                    searchIndexClient.index((String) event.get("payload_json"));
                } else {
                    searchIndexClient.delete((String) event.get("aggregate_id"));
                }
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
