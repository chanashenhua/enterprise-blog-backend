package com.company.blog.notification.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SubscriptionService {
    private final ContentSubscriptionRepository repository;
    private final SubscriptionCatalogClient catalogClient;

    public SubscriptionService(ContentSubscriptionRepository repository, SubscriptionCatalogClient catalogClient) {
        this.repository = repository;
        this.catalogClient = catalogClient;
    }

    @Transactional(readOnly = true)
    public List<ContentSubscription> list(String userId) {
        return repository.findByUser(requireUser(userId));
    }

    @Transactional
    public ContentSubscription subscribe(String userId, String type, String targetId) {
        String subscriber = requireUser(userId);
        SubscriptionTargetType targetType = SubscriptionTargetType.from(type);
        String target = requireTarget(targetId);
        if (!catalogClient.exists(targetType, target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription target does not exist or is disabled");
        }
        return repository.save(new ContentSubscription(
                UUID.randomUUID().toString(), subscriber, targetType, target, Instant.now()
        ));
    }

    @Transactional
    public void unsubscribe(String userId, String type, String targetId) {
        repository.delete(requireUser(userId), SubscriptionTargetType.from(type), requireTarget(targetId));
    }

    private static String requireUser(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
        return value.trim();
    }

    private static String requireTarget(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription target is required");
        }
        return value.trim();
    }
}
