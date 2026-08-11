package com.company.blog.article.api;

public interface ArticleSubscriptionNotificationClient {
    void send(String eventId, String payloadJson);
}
