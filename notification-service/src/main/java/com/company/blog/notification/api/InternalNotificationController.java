package com.company.blog.notification.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {
    private final NotificationService service;
    private final String internalToken;

    public InternalNotificationController(
            NotificationService service,
            @Value("${blog.internal.notification-token}") String internalToken
    ) {
        this.service = service;
        this.internalToken = internalToken;
    }

    @PostMapping
    public NotificationResponse create(
            @RequestHeader("X-Internal-Token") String token,
            @RequestBody CreateNotificationRequest request
    ) {
        if (!internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
        return service.create(request);
    }
}
