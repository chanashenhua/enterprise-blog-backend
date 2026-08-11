package com.company.blog.tag.api;

import java.util.List;

public interface TagAuditOutbox {
    void append(
            String actorId,
            List<String> actorRoles,
            String action,
            String resourceType,
            String resourceId,
            String details
    );

    static TagAuditOutbox noop() {
        return (actorId, actorRoles, action, resourceType, resourceId, details) -> { };
    }
}
