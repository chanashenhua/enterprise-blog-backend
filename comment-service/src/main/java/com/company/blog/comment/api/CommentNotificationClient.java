package com.company.blog.comment.api;

public interface CommentNotificationClient {
    void send(String eventId, String payloadJson);
}
