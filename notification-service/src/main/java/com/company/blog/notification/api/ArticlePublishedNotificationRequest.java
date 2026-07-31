package com.company.blog.notification.api;

import java.util.Set;

public record ArticlePublishedNotificationRequest(
        String eventId,
        String articleId,
        String authorId,
        String title,
        String categoryId,
        Set<String> tagIds
) {
    public ArticlePublishedNotificationRequest {
        tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
    }
}
