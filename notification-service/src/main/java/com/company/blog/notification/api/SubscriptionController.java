package com.company.blog.notification.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {
    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<ContentSubscription> list(@RequestHeader("X-User-Id") String userId) {
        return service.list(userId);
    }

    @PutMapping("/{type}/{targetId}")
    public ContentSubscription subscribe(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("type") String type,
            @PathVariable("targetId") String targetId
    ) {
        return service.subscribe(userId, type, targetId);
    }

    @DeleteMapping("/{type}/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("type") String type,
            @PathVariable("targetId") String targetId
    ) {
        service.unsubscribe(userId, type, targetId);
    }
}
