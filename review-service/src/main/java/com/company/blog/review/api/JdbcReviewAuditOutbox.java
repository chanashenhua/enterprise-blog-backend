package com.company.blog.review.api;

import com.company.blog.review.ReviewTicket;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcReviewAuditOutbox implements ReviewAuditOutbox {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final JdbcTemplate jdbcTemplate;

    public JdbcReviewAuditOutbox(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void append(
            String actorId,
            List<String> actorRoles,
            String action,
            ReviewTicket ticket,
            String details
    ) {
        try {
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventId", eventId);
            payload.put("sourceService", "review-service");
            payload.put("actorId", normalizedActor(actorId));
            payload.put("actorRoles", actorRoles == null ? List.of() : actorRoles);
            payload.put("action", action);
            payload.put("resourceType", "ARTICLE");
            payload.put("resourceId", ticket.articleId());
            payload.put("outcome", "SUCCESS");
            payload.put("details", "reviewTicket=" + ticket.id() + (details == null || details.isBlank() ? "" : "; " + details.trim()));
            payload.put("occurredAt", Instant.now().toString());
            jdbcTemplate.update(
                    """
                            insert into review_audit_event(id, payload_json, status, retry_count)
                            values (?, ?, 'PENDING', 0)
                            """,
                    eventId,
                    OBJECT_MAPPER.writeValueAsString(payload)
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to persist review audit event", ex);
        }
    }

    private static String normalizedActor(String actorId) {
        return actorId == null || actorId.isBlank() ? "unknown" : actorId.trim();
    }
}
