package com.company.blog.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class ElasticsearchIndexInitializer implements ApplicationRunner {
    private final RestClient client;
    private final ObjectMapper mapper;

    public ElasticsearchIndexInitializer(RestClient.Builder builder, ObjectMapper mapper,
            @Value("${blog.elasticsearch.base-url:http://elasticsearch:9200}") String baseUrl) {
        client = builder.baseUrl(baseUrl).build();
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        JsonNode definition;
        try (InputStream stream = new ClassPathResource("elasticsearch/article-index.json").getInputStream()) {
            definition = mapper.readTree(stream);
        }
        try {
            client.get().uri("/articles-v1").retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound missing) {
            try {
                client.put().uri("/articles-v1").body(definition).retrieve().toBodilessEntity();
            } catch (HttpClientErrorException.BadRequest race) {
                if (!race.getResponseBodyAsString().contains("resource_already_exists_exception")) throw race;
            }
        }
        // Additive mapping updates preserve all existing documents.
        client.put().uri("/articles-v1/_mapping").body(definition.path("mappings")).retrieve().toBodilessEntity();
    }
}
