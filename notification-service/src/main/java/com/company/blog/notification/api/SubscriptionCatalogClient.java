package com.company.blog.notification.api;

public interface SubscriptionCatalogClient {
    boolean exists(SubscriptionTargetType targetType, String targetId);
}
