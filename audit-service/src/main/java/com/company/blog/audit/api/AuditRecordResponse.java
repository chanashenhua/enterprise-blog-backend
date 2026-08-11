package com.company.blog.audit.api;

import com.company.blog.audit.AuditRecord;
import java.time.Instant;
import java.util.List;

public record AuditRecordResponse(
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
    static AuditRecordResponse from(AuditRecord record) {
        return new AuditRecordResponse(
                record.id(), record.eventId(), record.sourceService(), record.actorId(), record.actorRoles(),
                record.action(), record.resourceType(), record.resourceId(), record.outcome(), record.details(),
                record.traceId(), record.occurredAt(), record.createdAt()
        );
    }
}
