package com.company.blog.audit.api;

import com.company.blog.audit.AuditRecord;
import java.util.List;
import java.util.Optional;

public interface AuditRepository {
    AuditRecord save(AuditRecord record);

    Optional<AuditRecord> findByEventId(String eventId);

    List<AuditRecord> search(AuditQuery query);
}
