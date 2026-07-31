package com.company.blog.stats.api;

import java.time.Instant;

public record PersonalInteractionItem(
        String articleId,
        String interactionType,
        Instant interactedAt
) {
}
