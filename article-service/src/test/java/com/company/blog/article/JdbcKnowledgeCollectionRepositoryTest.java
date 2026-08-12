package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.article.api.JdbcArticleRepository;
import com.company.blog.article.api.JdbcKnowledgeCollectionRepository;
import com.company.blog.article.api.KnowledgeCollection;
import com.company.blog.article.api.StoredArticle;
import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import com.company.blog.article.domain.ArticleVisibilityType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcKnowledgeCollectionRepositoryTest {
    @Test
    void persistsArticleOrderAndSupportsOwnerQueriesAndDeletion() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:knowledge_collection_repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        JdbcArticleRepository articles = new JdbcArticleRepository(jdbc);
        articles.save(published("a-one"));
        articles.save(published("a-two"));
        JdbcKnowledgeCollectionRepository repository = new JdbcKnowledgeCollectionRepository(jdbc);
        Instant createdAt = Instant.parse("2026-08-11T09:00:00Z");
        repository.save(new KnowledgeCollection(
                "kc-java",
                "u-author",
                "Java 学习路径",
                "按顺序阅读",
                List.of("a-two", "a-one"),
                createdAt,
                createdAt.plusSeconds(60)
        ));

        JdbcKnowledgeCollectionRepository repositoryAfterRestart = new JdbcKnowledgeCollectionRepository(jdbc);
        KnowledgeCollection restored = repositoryAfterRestart.findById("kc-java").orElseThrow();

        assertThat(restored.articleIds()).containsExactly("a-two", "a-one");
        assertThat(repositoryAfterRestart.findByOwnerId("u-author", 10))
                .extracting(KnowledgeCollection::id)
                .containsExactly("kc-java");
        assertThat(repositoryAfterRestart.findRecent(10))
                .extracting(KnowledgeCollection::id)
                .containsExactly("kc-java");
        assertThat(repositoryAfterRestart.deleteById("kc-java")).isTrue();
        assertThat(repositoryAfterRestart.findById("kc-java")).isEmpty();
    }

    private static StoredArticle published(String id) {
        Article article = Article.draft(id, "u-author", id + " 标题");
        article.submitForPublish(ArticleVisibilityType.COMPANY, Set.of(), false);
        return new StoredArticle(
                article,
                "{}",
                new ArticleContentProjection("<p>正文</p>", "正文"),
                Set.of("java"),
                "engineering"
        );
    }
}
