package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.article.api.ArticleNotificationOutboxDispatcher;
import com.company.blog.article.api.JdbcArticleOutbox;
import com.company.blog.article.api.StoredArticle;
import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ArticleNotificationOutboxDispatcherTest {
    @Test
    void deliversReviewNotificationWithStableOutboxEventId() {
        JdbcTemplate jdbc = migratedJdbc("article_notification_success");
        new JdbcArticleOutbox(jdbc).appendAuthorNotification(
                article(),
                "REVIEW_APPROVED",
                "文章审核已通过",
                "文章已经发布"
        );
        List<String> eventIds = new ArrayList<>();
        ArticleNotificationOutboxDispatcher dispatcher = new ArticleNotificationOutboxDispatcher(
                jdbc,
                (eventId, payloadJson) -> {
                    eventIds.add(eventId);
                    assertThat(payloadJson).contains("\"recipientUserId\":\"u-author\"");
                },
                20,
                5
        );

        dispatcher.deliverPending();

        assertThat(eventIds).hasSize(1);
        assertThat(jdbc.queryForObject("select status from domain_event", String.class))
                .isEqualTo("DELIVERED");
    }

    @Test
    void keepsNotificationPendingWhenTheRemoteServiceIsUnavailable() {
        JdbcTemplate jdbc = migratedJdbc("article_notification_failure");
        new JdbcArticleOutbox(jdbc).appendAuthorNotification(
                article(),
                "REVIEW_REJECTED",
                "文章审核未通过",
                "文章已退回草稿"
        );
        ArticleNotificationOutboxDispatcher dispatcher = new ArticleNotificationOutboxDispatcher(
                jdbc,
                (eventId, payloadJson) -> {
                    throw new IllegalStateException("notification unavailable");
                },
                20,
                5
        );

        dispatcher.deliverPending();

        assertThat(jdbc.queryForObject("select retry_count from domain_event", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from domain_event", String.class)).isEqualTo("PENDING");
    }

    private static StoredArticle article() {
        return new StoredArticle(
                Article.draft("a-1", "u-author", "通知测试"),
                "{\"type\":\"doc\"}",
                new ArticleContentProjection("<p>正文</p>", "正文"),
                Set.of("java")
        );
    }

    private static JdbcTemplate migratedJdbc(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        return new JdbcTemplate(dataSource);
    }
}
