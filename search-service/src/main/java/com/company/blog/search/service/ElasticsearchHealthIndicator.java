package com.company.blog.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component("elasticsearch")
public class ElasticsearchHealthIndicator implements HealthIndicator {
    private final RestClient client;

    public ElasticsearchHealthIndicator(RestClient.Builder builder,
            @Value("${blog.elasticsearch.base-url:http://elasticsearch:9200}") String baseUrl) {
        client = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Health health() {
        try {
            JsonNode response = client.get().uri("/_cluster/health/articles-v1").retrieve().body(JsonNode.class);
            String status = response == null ? "unavailable" : response.path("status").asText();
            return ("green".equals(status) || "yellow".equals(status) ? Health.up() : Health.down())
                    .withDetail("clusterStatus", status).build();
        } catch (Exception failure) {
            return Health.down().build();
        }
    }
}
