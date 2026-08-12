package com.company.blog.article.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record KnowledgeCollection(
        String id,
        String ownerId,
        String title,
        String description,
        List<String> articleIds,
        Instant createdAt,
        Instant updatedAt
) {
    public KnowledgeCollection {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        description = description == null ? "" : description;
        articleIds = articleIds == null ? List.of() : List.copyOf(articleIds);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
