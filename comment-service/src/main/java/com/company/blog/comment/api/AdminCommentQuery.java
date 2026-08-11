package com.company.blog.comment.api;

import com.company.blog.comment.CommentStatus;

public record AdminCommentQuery(
        String articleId,
        String authorId,
        CommentStatus status,
        int limit
) {
}
