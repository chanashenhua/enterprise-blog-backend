package com.company.blog.audit;

import java.time.Instant;
import java.util.List;

public record AuditRecord(
        String id,
        String eventId,
        String sourceService,
        String actorId,
        List<String> actorRoles,
        String action,
        String resourceType,
        String resourceId,
        String outcome,
        String details,
        String traceId,
        Instant occurredAt,
        Instant createdAt
) {
    public AuditRecord {
        actorRoles = actorRoles == null ? List.of() : List.copyOf(actorRoles);
    }
}
