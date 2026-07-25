package com.company.blog.tag;

import java.time.Instant;

public record CatalogItem(
        String id,
        String name,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
