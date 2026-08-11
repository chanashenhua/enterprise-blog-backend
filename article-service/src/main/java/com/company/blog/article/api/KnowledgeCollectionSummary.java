package com.company.blog.article.api;

import java.time.Instant;

public record KnowledgeCollectionSummary(
        String id,
        String ownerId,
        String title,
        String description,
        int articleCount,
        Instant createdAt,
        Instant updatedAt,
        boolean editable
) {
}
