package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.article.api.JdbcArticleOutbox;
import com.company.blog.article.api.StoredArticle;
import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import com.company.blog.article.domain.ArticleVisibilityType;
import com.company.blog.article.domain.DomainEvent;
import java.util.List;
import java.util.Set;
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

    @Test
    void publishedIndexSnapshotContainsTheArticleCategory() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:article_outbox_category;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Article article = Article.draft("a-category", "u-author", "分类文章");
        article.submitForPublish(ArticleVisibilityType.COMPANY, Set.of(), false);
        StoredArticle storedArticle = new StoredArticle(
                article,
                "{\"type\":\"doc\"}",
                new ArticleContentProjection("<p>正文</p>", "正文"),
                Set.of("java"),
                "engineering"
        );

        new JdbcArticleOutbox(jdbc).appendArticleEvents(storedArticle, article.pullEvents());

        assertThat(jdbc.queryForObject(
                "select payload_json from domain_event where aggregate_id = 'a-category'",
                String.class
        )).contains("\"categoryId\":\"engineering\"");
    }
}
