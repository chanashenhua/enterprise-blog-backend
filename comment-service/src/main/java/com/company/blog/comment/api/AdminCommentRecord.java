package com.company.blog.comment.api;

import com.company.blog.comment.Comment;
import com.company.blog.comment.CommentStatus;
import java.time.Instant;

public record AdminCommentRecord(
        String id,
        String articleId,
        String parentId,
        String authorId,
        String content,
        CommentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminCommentRecord from(Comment comment) {
        return new AdminCommentRecord(
                comment.id(),
                comment.articleId(),
                comment.parentId(),
                comment.authorId(),
                comment.content(),
                comment.status(),
                comment.createdAt(),
                comment.updatedAt()
        );
    }
}
