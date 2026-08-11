package com.company.blog.notification.api;

import com.company.blog.notification.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 站内通知用例服务，统一处理上游幂等事件和员工已读状态。
 */
@Service
public class NotificationService {
    private static final int MAX_LIMIT = 100;

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NotificationResponse create(CreateNotificationRequest request) {
        requireRequest(request);
        Notification notification = new Notification(
                UUID.randomUUID().toString(),
                request.eventId().trim(),
                request.recipientUserId().trim(),
                request.type().trim(),
                request.title().trim(),
                request.content().trim(),
                normalize(request.resourceType()),
                normalize(request.resourceId()),
                null,
                Instant.now()
        );
        return NotificationResponse.from(repository.save(notification));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String userId, int requestedLimit) {
        String recipient = requireUser(userId);
        int limit = Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
        return repository.findByRecipient(recipient, limit).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(String userId) {
        return new UnreadCountResponse(repository.countUnread(requireUser(userId)));
    }

    @Transactional
    public NotificationResponse markRead(String userId, String notificationId) {
        return repository.markRead(requireText(notificationId, "Notification id is required"), requireUser(userId))
                .map(NotificationResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    @Transactional
    public UnreadCountResponse markAllRead(String userId) {
        String recipient = requireUser(userId);
        repository.markAllRead(recipient);
        return new UnreadCountResponse(repository.countUnread(recipient));
    }

    private static void requireRequest(CreateNotificationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification request is required");
        }
        requireText(request.eventId(), "Event id is required");
        requireText(request.recipientUserId(), "Recipient user id is required");
        requireText(request.type(), "Notification type is required");
        requireText(request.title(), "Notification title is required");
        requireText(request.content(), "Notification content is required");
    }

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
        return userId.trim();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
