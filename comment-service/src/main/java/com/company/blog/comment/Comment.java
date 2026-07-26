package com.company.blog.comment;

import java.time.Instant;

public record Comment(
        String id,
        String articleId,
        String parentId,
        String authorId,
        String content,
        CommentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean deleted() {
        return status == CommentStatus.DELETED;
    }
}
