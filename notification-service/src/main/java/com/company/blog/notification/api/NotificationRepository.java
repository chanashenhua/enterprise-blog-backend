package com.company.blog.notification.api;

import com.company.blog.notification.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);

    Optional<Notification> findByEventId(String eventId);

    List<Notification> findByRecipient(String recipientUserId, int limit);

    long countUnread(String recipientUserId);

    Optional<Notification> markRead(String id, String recipientUserId);

    int markAllRead(String recipientUserId);
}
