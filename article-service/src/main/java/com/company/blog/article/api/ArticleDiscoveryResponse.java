package com.company.blog.article.api;

import java.time.Instant;
import java.util.List;

public record ArticleDiscoveryResponse(
        DiscoveryTargetType targetType,
        String targetId,
        List<HomeFeedItem> items,
        Instant generatedAt
) {
    public ArticleDiscoveryResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
