package com.company.blog.article.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpArticleSubscriptionNotificationClient implements ArticleSubscriptionNotificationClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final String token;

    public HttpArticleSubscriptionNotificationClient(
            @Value("${blog.services.notification-service-url:http://notification-service:8091}") String baseUrl,
            @Value("${blog.internal.notification-token:local-notification-token}") String token
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.token = token;
    }

    @Override
    public void send(String eventId, String payloadJson) {
        try {
            Map<String, Object> articlePayload = new LinkedHashMap<>(
                    OBJECT_MAPPER.readValue(payloadJson, new TypeReference<>() { })
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventId", eventId);
            payload.put("articleId", articlePayload.get("articleId"));
            payload.put("authorId", articlePayload.get("authorId"));
            payload.put("title", articlePayload.get("title"));
            payload.put("categoryId", articlePayload.get("categoryId"));
            payload.put("tagIds", articlePayload.get("tags"));
            restClient.post()
                    .uri("/internal/notifications/article-published")
                    .header("X-Internal-Token", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to deliver article subscription notification", ex);
        }
    }
}
