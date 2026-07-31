package com.company.blog.audit.api;

import java.time.Instant;

public record AuditQuery(
        String actorId,
        String action,
        String resourceType,
        String resourceId,
        Instant from,
        Instant to,
        int limit
) {
}
