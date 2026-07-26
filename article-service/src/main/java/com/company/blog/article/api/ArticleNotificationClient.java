package com.company.blog.article.api;

public interface ArticleNotificationClient {
    void send(String eventId, String payloadJson);
}
