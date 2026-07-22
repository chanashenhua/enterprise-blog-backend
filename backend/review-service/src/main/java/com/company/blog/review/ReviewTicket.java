package com.company.blog.review;

import java.util.Set;

public record ReviewTicket(
        String id,
        String articleId,
        String reviewRequestId,
        String authorId,
        String visibilityType,
        Set<String> targetOrgIds,
        ReviewTicketStatus status
) {
    public ReviewTicket {
        targetOrgIds = targetOrgIds == null ? Set.of() : Set.copyOf(targetOrgIds);
    }
}
