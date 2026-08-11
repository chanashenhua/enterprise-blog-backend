package com.company.blog.comment.api;

import com.company.blog.comment.Comment;
import java.time.Instant;

public record CommentResponse(
        String id,
        String articleId,
        String parentId,
        String authorId,
        String content,
        boolean deleted,
        boolean hidden,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.id(),
                comment.articleId(),
                comment.parentId(),
                comment.authorId(),
                comment.active() ? comment.content() : null,
                comment.deleted(),
                comment.hidden(),
                comment.createdAt(),
                comment.updatedAt()
        );
    }
}
