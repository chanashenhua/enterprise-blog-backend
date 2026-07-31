package com.company.blog.notification.api;

public record NotificationTypeSummary(
        String type,
        long totalCount,
        long unreadCount
) {
}
