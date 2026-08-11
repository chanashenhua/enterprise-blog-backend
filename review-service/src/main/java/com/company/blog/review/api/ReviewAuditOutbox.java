package com.company.blog.review.api;

import com.company.blog.review.ReviewTicket;
import java.util.List;

public interface ReviewAuditOutbox {
    void append(
            String actorId,
            List<String> actorRoles,
            String action,
            ReviewTicket ticket,
            String details
    );

    static ReviewAuditOutbox noop() {
        return (actorId, actorRoles, action, ticket, details) -> { };
    }
}
