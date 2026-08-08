package com.company.blog.notification.api;

import java.util.List;

public record SubscriptionGovernanceOverview(
        long totalSubscriptionCount,
        long subscriberCount,
        long tagSubscriptionCount,
        long categorySubscriptionCount,
        List<SubscriptionTargetSummary> topTargets
) {
    public SubscriptionGovernanceOverview {
        topTargets = topTargets == null ? List.of() : List.copyOf(topTargets);
    }
}
