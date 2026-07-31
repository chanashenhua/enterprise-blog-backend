package com.company.blog.notification.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ContentSubscriptionRepository {
    ContentSubscription save(ContentSubscription subscription);

    Optional<ContentSubscription> find(String userId, SubscriptionTargetType targetType, String targetId);

    List<ContentSubscription> findByUser(String userId);

    void delete(String userId, SubscriptionTargetType targetType, String targetId);

    Set<String> findSubscriberUserIds(String categoryId, Set<String> tagIds);
}
