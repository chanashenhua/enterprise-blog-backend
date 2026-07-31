package com.company.blog.notification.api;

public record NotificationGovernanceQuery(
        String recipientUserId,
        String type,
        Boolean read,
        int limit
) {
}
