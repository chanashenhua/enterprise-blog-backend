package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.company.blog.article.api.JdbcArticleRepository;
import com.company.blog.article.api.StoredArticle;
import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import com.company.blog.article.domain.ArticleStatus;
import com.company.blog.article.domain.ArticleVisibilityType;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcArticleRepositoryTest {
    @Test
    void restoresTheCompleteArticleAfterRepositoryIsRecreated() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:article_repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        JdbcArticleRepository firstRepository = new JdbcArticleRepository(jdbcTemplate);
        String contentJson = """
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"持久化正文"}]}]}
                """.trim();
        Article article = Article.draft("a-restart", "u-author", "重启后仍可读取");
        article.requestReview(ArticleVisibilityType.TEAM, Set.of("t-search"));
        firstRepository.save(new StoredArticle(
                article,
                contentJson,
                ArticleContentProjection.from(contentJson),
                Set.of("postgresql", "java"),
                "engineering"
        ));
        firstRepository.appendContentVersion(
                firstRepository.findById("a-restart").orElseThrow(),
                "u-author"
        );

        JdbcArticleRepository repositoryAfterRestart = new JdbcArticleRepository(jdbcTemplate);
        StoredArticle restored = repositoryAfterRestart.findById("a-restart").orElseThrow();

        assertThat(restored.article().status()).isEqualTo(ArticleStatus.PENDING_REVIEW);
        assertThat(restored.article().visibilityType()).isEqualTo(ArticleVisibilityType.TEAM);
        assertThat(restored.article().visibilityTargetIds()).containsExactly("t-search");
        assertThat(restored.article().reviewRequestId()).isEqualTo(article.reviewRequestId());
        assertThat(restored.tagIds()).containsExactlyInAnyOrder("postgresql", "java");
        assertThat(restored.categoryId()).isEqualTo("engineering");
        assertThat(restored.contentJson()).isEqualTo(contentJson);
        assertThat(restored.content().plainText()).isEqualTo("持久化正文");
        assertThat(restored.article().createdAt())
                .isCloseTo(article.createdAt(), within(1, ChronoUnit.MICROS));
        assertThat(restored.article().updatedAt())
                .isCloseTo(article.updatedAt(), within(1, ChronoUnit.MICROS));
        assertThat(repositoryAfterRestart.findContentVersions("a-restart"))
                .singleElement()
                .satisfies(version -> {
                    assertThat(version.versionNo()).isEqualTo(1);
                    assertThat(version.title()).isEqualTo("重启后仍可读取");
                    assertThat(version.tagIds()).containsExactlyInAnyOrder("postgresql", "java");
                    assertThat(version.categoryId()).isEqualTo("engineering");
                });
    }
}
