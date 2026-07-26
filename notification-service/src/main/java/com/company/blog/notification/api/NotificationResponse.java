package com.company.blog.notification.api;

import com.company.blog.notification.Notification;
import java.time.Instant;

public record NotificationResponse(
        String id,
        String type,
        String title,
        String content,
        String resourceType,
        String resourceId,
        boolean read,
        Instant createdAt
) {
    static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.type(),
                notification.title(),
                notification.content(),
                notification.resourceType(),
                notification.resourceId(),
                notification.read(),
                notification.createdAt()
        );
    }
}
