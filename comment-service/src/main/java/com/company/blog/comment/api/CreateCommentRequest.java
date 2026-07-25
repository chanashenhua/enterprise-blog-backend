package com.company.blog.comment.api;

public record CreateCommentRequest(String content, String parentId) {
}
