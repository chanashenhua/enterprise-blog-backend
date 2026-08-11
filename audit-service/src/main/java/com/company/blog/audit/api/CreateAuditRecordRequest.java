package com.company.blog.audit.api;

import java.time.Instant;
import java.util.List;

public record CreateAuditRecordRequest(
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
        Instant occurredAt
) {
}
