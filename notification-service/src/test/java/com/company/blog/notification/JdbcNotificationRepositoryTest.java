package com.company.blog.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.notification.api.JdbcNotificationRepository;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcNotificationRepositoryTest {
    @Test
    void migrationRepositoryAndEventDeduplicationWorkTogether() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:notification_repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcNotificationRepository repository = new JdbcNotificationRepository(new JdbcTemplate(dataSource));
        Notification notification = new Notification(
                "n-1",
                "event-1",
                "u-author",
                "REVIEW_APPROVED",
                "审核通过",
                "文章已经发布",
                "ARTICLE",
                "a-1",
                null,
                Instant.now()
        );

        assertThat(repository.save(notification).id()).isEqualTo("n-1");
        assertThat(repository.save(new Notification(
                "n-duplicate",
                "event-1",
                "u-author",
                "REVIEW_APPROVED",
                "重复",
                "重复",
                "ARTICLE",
                "a-1",
                null,
                Instant.now()
        )).id()).isEqualTo("n-1");
        assertThat(repository.countUnread("u-author")).isEqualTo(1);
        assertThat(repository.markRead("n-1", "u-author")).get().extracting(Notification::read).isEqualTo(true);
        assertThat(repository.countUnread("u-author")).isZero();
    }
}
