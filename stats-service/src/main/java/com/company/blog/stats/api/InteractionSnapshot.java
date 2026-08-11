package com.company.blog.stats.api;

public record InteractionSnapshot(
        long viewCount,
        long likeCount,
        long favoriteCount,
        boolean liked,
        boolean favorited
) {
}
