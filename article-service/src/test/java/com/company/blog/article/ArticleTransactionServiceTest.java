package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.article.api.ArticleOutbox;
import com.company.blog.article.api.ArticleRepository;
import com.company.blog.article.api.ArticleTransactionService;
import com.company.blog.article.api.JdbcArticleOutbox;
import com.company.blog.article.api.JdbcArticleRepository;
import com.company.blog.article.api.StoredArticle;
import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import com.company.blog.article.domain.ArticleStatus;
import com.company.blog.article.domain.ArticleVisibilityType;
import java.util.Set;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringJUnitConfig(ArticleTransactionServiceTest.Config.class)
class ArticleTransactionServiceTest {
    @Autowired
    private ArticleTransactionService transactionService;

    @Autowired
    private ArticleRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rollsBackArticleAndOutboxTogetherWhenEventHandlingFails() {
        String contentJson = """
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"事务一致性"}]}]}
                """.trim();
        transactionService.saveDraft(new StoredArticle(
                Article.draft("a-transaction", "u-author", "事务一致性"),
                contentJson,
                ArticleContentProjection.from(contentJson),
                Set.of("postgresql")
        ));

        assertThatThrownBy(() -> transactionService.publish(
                "a-transaction",
                ArticleVisibilityType.COMPANY,
                Set.of()
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("simulated outbox failure");

        StoredArticle restored = repository.findById("a-transaction").orElseThrow();
        assertThat(restored.article().status()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from domain_event where aggregate_id = ?",
                Integer.class,
                "a-transaction"
        )).isZero();
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    "jdbc:h2:mem:article_transaction;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                    "sa",
                    ""
            );
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            return dataSource;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        ArticleRepository articleRepository(JdbcTemplate jdbcTemplate) {
            return new JdbcArticleRepository(jdbcTemplate);
        }

        @Bean
        ArticleOutbox articleOutbox(JdbcTemplate jdbcTemplate) {
            JdbcArticleOutbox delegate = new JdbcArticleOutbox(jdbcTemplate);
            return new ArticleOutbox() {
                @Override
                public void appendArticleEvents(java.util.List<com.company.blog.article.domain.DomainEvent> events) {
                    delegate.appendArticleEvents(events);
                    throw new IllegalStateException("simulated outbox failure");
                }

                @Override
                public void appendArticleEvents(
                        StoredArticle article,
                        java.util.List<com.company.blog.article.domain.DomainEvent> events
                ) {
                    delegate.appendArticleEvents(article, events);
                    throw new IllegalStateException("simulated outbox failure");
                }
            };
        }

        @Bean
        ArticleTransactionService articleTransactionService(
                ArticleRepository repository,
                ArticleOutbox articleOutbox
        ) {
            return new ArticleTransactionService(repository, articleOutbox);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
