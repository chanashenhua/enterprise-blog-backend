package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.article.api.ArticleSearchIndexClient;
import com.company.blog.article.api.ArticleSearchOutboxDispatcher;
import com.company.blog.article.api.JdbcArticleOutbox;
import com.company.blog.article.domain.DomainEvent;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ArticleSearchOutboxDispatcherTest {
    @Test
    void incrementsRetryAndKeepsEventPendingWhenIndexingFails() {
        JdbcTemplate jdbc = migratedJdbc("search_dispatcher_failure");
        new JdbcArticleOutbox(jdbc).appendArticleEvents(List.of(DomainEvent.articlePublished("a-1")));
        ArticleSearchOutboxDispatcher dispatcher = new ArticleSearchOutboxDispatcher(jdbc, payload -> {
            throw new IllegalStateException("search unavailable");
        }, 20, 5);

        dispatcher.deliverPending();

        assertThat(jdbc.queryForObject("select retry_count from domain_event", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from domain_event", String.class)).isEqualTo("PENDING");
    }

    @Test
    void marksPublishedEventDeliveredAfterSearchIndexAcceptsIt() {
        JdbcTemplate jdbc = migratedJdbc("search_dispatcher_success");
        new JdbcArticleOutbox(jdbc).appendArticleEvents(List.of(DomainEvent.articlePublished("a-1")));
        ArticleSearchOutboxDispatcher dispatcher = new ArticleSearchOutboxDispatcher(jdbc, payload -> { }, 20, 5);

        dispatcher.deliverPending();

        assertThat(jdbc.queryForObject("select status from domain_event", String.class)).isEqualTo("DELIVERED");
    }

    @Test
    void removesSearchDocumentForWithdrawAndDeleteEvents() {
        JdbcTemplate jdbc = migratedJdbc("search_dispatcher_delete");
        new JdbcArticleOutbox(jdbc).appendArticleEvents(List.of(
                DomainEvent.articleWithdrawn("a-withdrawn"),
                DomainEvent.articleDeleted("a-deleted")
        ));
        List<String> deletedIds = new ArrayList<>();
        ArticleSearchIndexClient client = new ArticleSearchIndexClient() {
            @Override
            public void index(String payloadJson) {
            }

            @Override
            public void delete(String articleId) {
                deletedIds.add(articleId);
            }
        };
        ArticleSearchOutboxDispatcher dispatcher = new ArticleSearchOutboxDispatcher(jdbc, client, 20, 5);

        dispatcher.deliverPending();

        assertThat(deletedIds).containsExactly("a-withdrawn", "a-deleted");
        assertThat(jdbc.queryForObject(
                "select count(*) from domain_event where status = 'DELIVERED'",
                Integer.class
        )).isEqualTo(2);
    }

    private static JdbcTemplate migratedJdbc(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        return new JdbcTemplate(dataSource);
    }
}
