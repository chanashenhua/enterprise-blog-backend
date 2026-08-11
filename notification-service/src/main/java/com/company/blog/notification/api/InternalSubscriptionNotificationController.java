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
@RequestMapping("/internal/notifications/article-published")
public class InternalSubscriptionNotificationController {
    private final SubscriptionNotificationService service;
    private final String internalToken;

    public InternalSubscriptionNotificationController(
            SubscriptionNotificationService service,
            @Value("${blog.internal.notification-token}") String internalToken
    ) {
        this.service = service;
        this.internalToken = internalToken;
    }

    @PostMapping
    public ArticlePublishedNotificationResult notifySubscribers(
            @RequestHeader("X-Internal-Token") String token,
            @RequestBody ArticlePublishedNotificationRequest request
    ) {
        if (!internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
        return service.notifySubscribers(request);
    }
}
