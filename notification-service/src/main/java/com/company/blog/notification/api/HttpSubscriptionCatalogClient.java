package com.company.blog.notification.api;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpSubscriptionCatalogClient implements SubscriptionCatalogClient {
    private final RestClient restClient;

    public HttpSubscriptionCatalogClient(
            @Value("${blog.services.tag-service-url:http://tag-service:8085}") String baseUrl
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public boolean exists(SubscriptionTargetType targetType, String targetId) {
        String path = targetType == SubscriptionTargetType.TAG ? "/api/tags" : "/api/categories";
        List<Map<String, Object>> items = restClient.get()
                .uri(path)
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        return items != null && items.stream().anyMatch(item -> targetId.equals(item.get("id")));
    }
}
