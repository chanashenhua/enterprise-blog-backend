package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.article.api.ArticleSubscriptionNotificationOutboxDispatcher;
import com.company.blog.article.api.JdbcArticleOutbox;
import com.company.blog.article.api.StoredArticle;
import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import com.company.blog.article.domain.ArticleVisibilityType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ArticleSubscriptionNotificationOutboxDispatcherTest {
    @Test
    void deliversCompanyArticleWithStableEventIdAndCompleteSubscriptionSnapshot() {
        JdbcTemplate jdbc = migratedJdbc("subscription_notification_success");
        appendPublished(jdbc, ArticleVisibilityType.COMPANY);
        List<String> eventIds = new ArrayList<>();
        ArticleSubscriptionNotificationOutboxDispatcher dispatcher =
                new ArticleSubscriptionNotificationOutboxDispatcher(
                        jdbc,
                        (eventId, payloadJson) -> {
                            eventIds.add(eventId);
                            assertThat(payloadJson)
                                    .contains("\"title\":\"订阅通知测试\"")
                                    .contains("\"tags\":[\"java\"]")
                                    .contains("\"categoryId\":\"backend\"");
                        },
                        20,
                        5
                );

        dispatcher.deliverPending();

        assertThat(eventIds).hasSize(1);
        assertThat(jdbc.queryForObject(
                "select status from article_subscription_notification_event", String.class
        )).isEqualTo("DELIVERED");
    }

    @Test
    void doesNotCreateSubscriptionNotificationForRestrictedArticle() {
        JdbcTemplate jdbc = migratedJdbc("subscription_notification_restricted");
        appendPublished(jdbc, ArticleVisibilityType.DEPARTMENT);

        assertThat(jdbc.queryForObject(
                "select count(*) from article_subscription_notification_event", Integer.class
        )).isZero();
    }

    private static void appendPublished(JdbcTemplate jdbc, ArticleVisibilityType visibilityType) {
        Article article = Article.draft("article-1", "u-author", "订阅通知测试");
        Set<String> targets = visibilityType == ArticleVisibilityType.COMPANY ? Set.of() : Set.of("d-platform");
        article.submitForPublish(visibilityType, targets, false);
        StoredArticle storedArticle = new StoredArticle(
                article,
                "{\"type\":\"doc\"}",
                new ArticleContentProjection("<p>正文</p>", "正文"),
                Set.of("java"),
                "backend"
        );
        new JdbcArticleOutbox(jdbc).appendArticleEvents(storedArticle, article.pullEvents());
    }

    private static JdbcTemplate migratedJdbc(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        return new JdbcTemplate(dataSource);
    }
}
