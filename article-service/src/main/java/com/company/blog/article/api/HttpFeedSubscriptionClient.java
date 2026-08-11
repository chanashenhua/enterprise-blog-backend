package com.company.blog.article.api;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpFeedSubscriptionClient implements FeedSubscriptionClient {
    private final RestClient restClient;

    public HttpFeedSubscriptionClient(
            @Value("${blog.services.notification-service-url:http://notification-service:8091}") String baseUrl
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public List<FeedSubscription> findByUser(String userId) {
        FeedSubscription[] response = restClient.get()
                .uri("/api/subscriptions")
                .header("X-User-Id", userId)
                .retrieve()
                .body(FeedSubscription[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }
}
