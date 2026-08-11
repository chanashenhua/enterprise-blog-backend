package com.company.blog.comment.api;

import com.company.blog.comment.Comment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcCommentGovernanceAuditOutbox implements CommentGovernanceAuditOutbox {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final JdbcTemplate jdbcTemplate;

    public JdbcCommentGovernanceAuditOutbox(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void append(String actorId, List<String> actorRoles, String action, Comment comment, String reason) {
        try {
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventId", eventId);
            payload.put("sourceService", "comment-service");
            payload.put("actorId", actorId);
            payload.put("actorRoles", actorRoles == null ? List.of() : actorRoles);
            payload.put("action", action);
            payload.put("resourceType", "COMMENT");
            payload.put("resourceId", comment.id());
            payload.put("outcome", "SUCCESS");
            payload.put("details", details(comment, reason));
            payload.put("occurredAt", Instant.now().toString());
            jdbcTemplate.update(
                    """
                            insert into comment_audit_event(id, payload_json, status, retry_count)
                            values (?, ?, 'PENDING', 0)
                            """,
                    eventId,
                    OBJECT_MAPPER.writeValueAsString(payload)
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to persist comment governance audit event", ex);
        }
    }

    private static String details(Comment comment, String reason) {
        String base = "articleId=" + comment.articleId() + "; authorId=" + comment.authorId();
        return reason == null || reason.isBlank() ? base : base + "; reason=" + reason.trim();
    }
}
