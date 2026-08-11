package com.company.blog.notification.api;

public record SubscriptionTargetSummary(
        SubscriptionTargetType targetType,
        String targetId,
        long subscriberCount
) {
}
