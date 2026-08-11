package com.company.blog.notification.api;

import java.time.Instant;

public record AdminNotificationRecord(
        String id,
        String eventId,
        String recipientUserId,
        String type,
        String title,
        String content,
        String resourceType,
        String resourceId,
        boolean read,
        Instant createdAt
) {
}
