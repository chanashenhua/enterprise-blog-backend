package com.company.blog.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.notification.api.ContentSubscription;
import com.company.blog.notification.api.JdbcContentSubscriptionRepository;
import com.company.blog.notification.api.SubscriptionTargetType;
import java.time.Instant;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcContentSubscriptionRepositoryTest {
    @Test
    void persistsSubscriptionsIdempotentlyAndFindsDistinctMatchingUsers() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:content_subscription_repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcContentSubscriptionRepository repository = new JdbcContentSubscriptionRepository(
                new JdbcTemplate(dataSource)
        );

        ContentSubscription java = new ContentSubscription(
                "s-1", "u-reader", SubscriptionTargetType.TAG, "java", Instant.now()
        );
        repository.save(java);
        ContentSubscription duplicate = repository.save(new ContentSubscription(
                "s-2", "u-reader", SubscriptionTargetType.TAG, "java", Instant.now()
        ));
        repository.save(new ContentSubscription(
                "s-3", "u-reader", SubscriptionTargetType.CATEGORY, "backend", Instant.now()
        ));

        assertThat(duplicate.id()).isEqualTo("s-1");
        assertThat(repository.findByUser("u-reader")).hasSize(2);
        assertThat(repository.findSubscriberUserIds("backend", Set.of("java")))
                .containsExactly("u-reader");
        assertThat(repository.overview().totalSubscriptionCount()).isEqualTo(2);
        assertThat(repository.overview().subscriberCount()).isEqualTo(1);
        assertThat(repository.overview().topTargets()).hasSize(2);
    }
}
