package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.article.api.JdbcArticleOutbox;
import com.company.blog.article.domain.DomainEvent;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcArticleOutboxTest {
    @Test
    void writesArticlePublishedEventToDomainEventTable() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:article_outbox;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        JdbcArticleOutbox outbox = new JdbcArticleOutbox(jdbc);
        outbox.appendArticleEvents(List.of(DomainEvent.articlePublished("a-1")));

        assertThat(jdbc.queryForObject("select event_type from domain_event where aggregate_id = 'a-1'", String.class))
                .isEqualTo("ArticlePublished");
        assertThat(jdbc.queryForObject("select status from domain_event where aggregate_id = 'a-1'", String.class))
                .isEqualTo("PENDING");
    }
}