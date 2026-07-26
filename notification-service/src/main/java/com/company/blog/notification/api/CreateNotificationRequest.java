package com.company.blog.notification.api;

public record CreateNotificationRequest(
        String eventId,
        String recipientUserId,
        String type,
        String title,
        String content,
        String resourceType,
        String resourceId
) {
}
