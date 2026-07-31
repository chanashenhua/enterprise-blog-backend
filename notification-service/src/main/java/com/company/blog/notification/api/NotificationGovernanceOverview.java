package com.company.blog.notification.api;

import java.util.List;

public record NotificationGovernanceOverview(
        long totalCount,
        long unreadCount,
        long readCount,
        long recipientCount,
        List<NotificationTypeSummary> typeSummaries
) {
    public NotificationGovernanceOverview {
        typeSummaries = typeSummaries == null ? List.of() : List.copyOf(typeSummaries);
    }
}
