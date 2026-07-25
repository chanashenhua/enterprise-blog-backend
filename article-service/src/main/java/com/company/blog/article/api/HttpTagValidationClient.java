package com.company.blog.article.api;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class HttpTagValidationClient implements TagValidationClient {
    private final RestClient restClient;

    public HttpTagValidationClient(@Value("${blog.services.tag-service-url:http://tag-service:8085}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public void validate(Set<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        TagValidationResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/tags/validate")
                        .queryParam("ids", tagIds.toArray())
                        .build())
                .retrieve()
                .body(TagValidationResponse.class);
        if (response == null || !response.valid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown article tags");
        }
    }

    @Override
    public void validateCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return;
        }
        CatalogValidationResponse response = restClient.get()
                .uri("/internal/categories/{id}/validate", categoryId)
                .retrieve()
                .body(CatalogValidationResponse.class);
        if (response == null || !response.valid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or inactive article category");
        }
    }
}
