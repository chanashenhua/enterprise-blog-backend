package com.company.blog.notification.api;

import java.time.Instant;

public record ContentSubscription(
        String id,
        String userId,
        SubscriptionTargetType targetType,
        String targetId,
        Instant createdAt
) {
}
