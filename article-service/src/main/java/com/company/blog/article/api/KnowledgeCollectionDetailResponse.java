package com.company.blog.article.api;

import java.time.Instant;
import java.util.List;

public record KnowledgeCollectionDetailResponse(
        String id,
        String ownerId,
        String title,
        String description,
        List<HomeFeedItem> articles,
        Instant createdAt,
        Instant updatedAt,
        boolean editable
) {
    public KnowledgeCollectionDetailResponse {
        articles = articles == null ? List.of() : List.copyOf(articles);
    }
}
