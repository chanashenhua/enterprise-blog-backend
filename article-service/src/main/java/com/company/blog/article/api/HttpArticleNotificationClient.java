package com.company.blog.article.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpArticleNotificationClient implements ArticleNotificationClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final String internalToken;

    public HttpArticleNotificationClient(
            @Value("${blog.services.notification-service-url:http://notification-service:8091}") String baseUrl,
            @Value("${blog.internal.notification-token}") String internalToken
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.internalToken = internalToken;
    }

    @Override
    public void send(String eventId, String payloadJson) {
        try {
            Map<String, Object> request = new LinkedHashMap<>(
                    OBJECT_MAPPER.readValue(payloadJson, new TypeReference<>() {
                    })
            );
            request.put("eventId", eventId);
            restClient.post()
                    .uri("/internal/notifications")
                    .header("X-Internal-Token", internalToken)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to deliver article notification", ex);
        }
    }
}
