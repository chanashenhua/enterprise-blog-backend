package com.company.blog.article.api;

import java.time.Instant;
import java.util.List;

public record HomeFeedResponse(
        List<HomeFeedItem> latest,
        List<HomeFeedItem> popular,
        List<HomeFeedItem> subscribed,
        Instant generatedAt
) {
    public HomeFeedResponse {
        latest = latest == null ? List.of() : List.copyOf(latest);
        popular = popular == null ? List.of() : List.copyOf(popular);
        subscribed = subscribed == null ? List.of() : List.copyOf(subscribed);
    }
}
