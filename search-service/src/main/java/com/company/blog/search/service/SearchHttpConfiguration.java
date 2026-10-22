package com.company.blog.search.service;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class SearchHttpConfiguration {
    @Bean
    RestClientCustomizer boundedSearchRequests() {
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(3000);
            factory.setReadTimeout(10000);
            builder.requestFactory(factory);
        };
    }
}
