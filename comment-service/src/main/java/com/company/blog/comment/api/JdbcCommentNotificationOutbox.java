package com.company.blog.comment.api;

import com.company.blog.comment.Comment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 评论回复通知的本地 Outbox，与评论写入共享同一个数据库事务。
 */
@Component
public class JdbcCommentNotificationOutbox implements CommentNotificationOutbox {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    public JdbcCommentNotificationOutbox(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void appendReplyNotification(Comment reply, Comment parent) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("recipientUserId", parent.authorId());
            payload.put("type", "COMMENT_REPLY");
            payload.put("title", "收到新的评论回复");
            payload.put("content", reply.authorId() + " 回复了你的评论：" + summary(reply.content()));
            payload.put("resourceType", "ARTICLE");
            payload.put("resourceId", reply.articleId());
            jdbcTemplate.update(
                    """
                            insert into comment_notification_event(
                                id, comment_id, payload_json, status, retry_count
                            ) values (?, ?, ?, 'PENDING', 0)
                            """,
                    UUID.randomUUID().toString(),
                    reply.id(),
                    OBJECT_MAPPER.writeValueAsString(payload)
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to persist comment notification event", ex);
        }
    }

    private static String summary(String content) {
        return content.length() <= 120 ? content : content.substring(0, 120);
    }
}
