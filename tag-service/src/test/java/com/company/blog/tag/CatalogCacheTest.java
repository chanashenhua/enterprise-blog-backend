package com.company.blog.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.blog.tag.api.CatalogItemRequest;
import com.company.blog.tag.api.CatalogRepository;
import com.company.blog.tag.api.CatalogService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class CatalogCacheTest {
    @Test
    void cachesReadsAndEvictsAllCatalogViewsAfterAMutation() {
        CatalogRepository repository = mock(CatalogRepository.class);
        Instant now = Instant.now();
        CatalogItem java = new CatalogItem("java", "Java", true, now, now);
        CatalogItem kafka = new CatalogItem("kafka", "Kafka", true, now, now);
        when(repository.findAll(CatalogType.TAG, false)).thenReturn(List.of(java), List.of(java, kafka));
        when(repository.create(CatalogType.TAG, "kafka", "Kafka")).thenReturn(kafka);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(CatalogRepository.class, () -> repository);
            context.register(CacheTestConfiguration.class);
            context.refresh();
            CatalogService service = context.getBean(CatalogService.class);

            assertThat(service.list(CatalogType.TAG, false)).hasSize(1);
            assertThat(service.list(CatalogType.TAG, false)).hasSize(1);
            verify(repository, times(1)).findAll(CatalogType.TAG, false);

            service.create(CatalogType.TAG, new CatalogItemRequest("kafka", "Kafka"));

            assertThat(service.list(CatalogType.TAG, false)).hasSize(2);
            verify(repository, times(2)).findAll(CatalogType.TAG, false);
        }
    }

    @Test
    void evictsCategoryValidationAfterDeactivation() {
        CatalogRepository repository = mock(CatalogRepository.class);
        Instant now = Instant.now();
        CatalogItem active = new CatalogItem("engineering", "工程实践", true, now, now);
        CatalogItem inactive = new CatalogItem("engineering", "工程实践", false, now, now);
        when(repository.findById(CatalogType.CATEGORY, "engineering"))
                .thenReturn(Optional.of(active), Optional.of(inactive));
        when(repository.deactivate(CatalogType.CATEGORY, "engineering")).thenReturn(Optional.of(inactive));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(CatalogRepository.class, () -> repository);
            context.register(CacheTestConfiguration.class);
            context.refresh();
            CatalogService service = context.getBean(CatalogService.class);

            assertThat(service.validateCategory("engineering").valid()).isTrue();
            assertThat(service.validateCategory("engineering").valid()).isTrue();
            verify(repository, times(1)).findById(CatalogType.CATEGORY, "engineering");

            service.deactivate(CatalogType.CATEGORY, "engineering");

            assertThat(service.validateCategory("engineering").valid()).isFalse();
            verify(repository, times(2)).findById(CatalogType.CATEGORY, "engineering");
        }
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfiguration {
        @Bean
        CatalogService catalogService(CatalogRepository repository) {
            return new CatalogService(repository);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
