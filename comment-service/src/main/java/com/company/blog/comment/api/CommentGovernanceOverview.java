package com.company.blog.comment.api;

public record CommentGovernanceOverview(
        long totalCount,
        long activeCount,
        long hiddenCount,
        long deletedCount,
        long articleCount,
        long authorCount
) {
}
