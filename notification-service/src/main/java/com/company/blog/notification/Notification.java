package com.company.blog.notification;

import java.time.Instant;

public record Notification(
        String id,
        String eventId,
        String recipientUserId,
        String type,
        String title,
        String content,
        String resourceType,
        String resourceId,
        Instant readAt,
        Instant createdAt
) {
    public boolean read() {
        return readAt != null;
    }
}
