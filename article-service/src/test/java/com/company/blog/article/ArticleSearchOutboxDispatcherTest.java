package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.company.blog.article.api.ArticleSearchSynchronizationService;
import com.company.blog.article.api.ArticleSearchOutboxDispatcher;
import com.company.blog.article.api.JdbcArticleOutbox;
import com.company.blog.article.domain.DomainEvent;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ArticleSearchOutboxDispatcherTest {
    @Test
    void keepsDispatcherEnabledWhenSettingIsAbsent() {
        dispatcherContext().run(context ->
                assertThat(context).hasSingleBean(ArticleSearchOutboxDispatcher.class));
    }

    @Test
    void canDisableSearchDeliveryWithoutTouchingDatabaseOrSearch() {
        dispatcherContext().withPropertyValues("blog.outbox.search.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(ArticleSearchOutboxDispatcher.class);
            verifyNoInteractions(context.getBean(DataSource.class), context.getBean(ArticleSearchSynchronizationService.class));
        });
    }

    private static ApplicationContextRunner dispatcherContext() {
        DataSource dataSource = mock(DataSource.class);
        return new ApplicationContextRunner()
                .withBean(DataSource.class, () -> dataSource)
                .withBean(JdbcTemplate.class, () -> new JdbcTemplate(dataSource))
                .withBean(ArticleSearchSynchronizationService.class, () -> mock(ArticleSearchSynchronizationService.class))
                .withUserConfiguration(ArticleSearchOutboxDispatcher.class);
    }

    @Test
    void incrementsRetryAndKeepsEventPendingWhenIndexingFails() {
        JdbcTemplate jdbc = migratedJdbc("search_dispatcher_failure");
        new JdbcArticleOutbox(jdbc).appendArticleEvents(List.of(DomainEvent.articlePublished("a-1")));
        ArticleSearchSynchronizationService sync = mock(ArticleSearchSynchronizationService.class);
        doThrow(new IllegalStateException("search unavailable")).when(sync).synchronize("a-1");
        ArticleSearchOutboxDispatcher dispatcher = new ArticleSearchOutboxDispatcher(jdbc, sync, 20, 5);

        dispatcher.deliverPending();

        assertThat(jdbc.queryForObject("select retry_count from domain_event", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from domain_event", String.class)).isEqualTo("PENDING");
    }

    @Test
    void marksPublishedEventDeliveredAfterSearchIndexAcceptsIt() {
        JdbcTemplate jdbc = migratedJdbc("search_dispatcher_success");
        new JdbcArticleOutbox(jdbc).appendArticleEvents(List.of(DomainEvent.articlePublished("a-1")));
        ArticleSearchOutboxDispatcher dispatcher = new ArticleSearchOutboxDispatcher(jdbc, mock(ArticleSearchSynchronizationService.class), 20, 5);

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
        ArticleSearchSynchronizationService sync = mock(ArticleSearchSynchronizationService.class);
        ArticleSearchOutboxDispatcher dispatcher = new ArticleSearchOutboxDispatcher(jdbc, sync, 20, 5);

        dispatcher.deliverPending();

        verify(sync).synchronize("a-withdrawn");
        verify(sync).synchronize("a-deleted");
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
