package com.company.blog.comment.api;

public interface CommentAuditClient {
    void send(String payloadJson);
}
