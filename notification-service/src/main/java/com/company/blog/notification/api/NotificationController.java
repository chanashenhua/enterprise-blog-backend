package com.company.blog.notification.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationResponse> list(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return service.list(userId, limit);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@RequestHeader("X-User-Id") String userId) {
        return service.unreadCount(userId);
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markRead(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("notificationId") String notificationId
    ) {
        return service.markRead(userId, notificationId);
    }

    @PutMapping("/read-all")
    public UnreadCountResponse markAllRead(@RequestHeader("X-User-Id") String userId) {
        return service.markAllRead(userId);
    }
}
