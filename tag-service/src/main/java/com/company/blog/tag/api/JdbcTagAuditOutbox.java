package com.company.blog.tag.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcTagAuditOutbox implements TagAuditOutbox {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final JdbcTemplate jdbcTemplate;

    public JdbcTagAuditOutbox(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void append(
            String actorId,
            List<String> actorRoles,
            String action,
            String resourceType,
            String resourceId,
            String details
    ) {
        try {
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventId", eventId);
            payload.put("sourceService", "tag-service");
            payload.put("actorId", actorId == null || actorId.isBlank() ? "unknown" : actorId.trim());
            payload.put("actorRoles", actorRoles == null ? List.of() : actorRoles);
            payload.put("action", action);
            payload.put("resourceType", resourceType);
            payload.put("resourceId", resourceId);
            payload.put("outcome", "SUCCESS");
            payload.put("details", details);
            payload.put("occurredAt", Instant.now().toString());
            jdbcTemplate.update(
                    """
                            insert into tag_audit_event(id, payload_json, status, retry_count)
                            values (?, ?, 'PENDING', 0)
                            """,
                    eventId,
                    OBJECT_MAPPER.writeValueAsString(payload)
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to persist taxonomy audit event", ex);
        }
    }
}
