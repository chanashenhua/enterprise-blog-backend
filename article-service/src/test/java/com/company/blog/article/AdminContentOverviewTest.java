package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.article.api.AdminContentOverviewController;
import com.company.blog.article.api.AdminContentOverviewService;
import com.company.blog.article.api.ContentOperationsOverview;
import com.company.blog.article.api.JdbcAdminContentOverviewRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminContentOverviewTest {
    @Test
    void summarizesArticleCoverageAndKnowledgeCollections() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:admin_content_overview;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        JdbcArticleRepository articles = new JdbcArticleRepository(jdbc);
        articles.save(published("a-categorized", Set.of("java"), "engineering"));
        articles.save(published("a-plain", Set.of(), null));
        articles.save(stored(Article.draft("a-draft", "u-author", "草稿"), Set.of(), null));
        Article pending = Article.draft("a-pending", "u-author", "待审核");
        pending.requestReview(ArticleVisibilityType.TEAM, Set.of("t-search"));
        articles.save(stored(pending, Set.of("java"), "engineering"));
        Instant now = Instant.parse("2026-08-11T10:00:00Z");
        new JdbcKnowledgeCollectionRepository(jdbc).save(new KnowledgeCollection(
                "kc-overview",
                "u-author",
                "运营专题",
                "用于概览测试",
                List.of("a-categorized", "a-plain"),
                now,
                now
        ));

        ContentOperationsOverview overview = new JdbcAdminContentOverviewRepository(jdbc).overview();

        assertThat(overview.totalArticleCount()).isEqualTo(4);
        assertThat(overview.publishedArticleCount()).isEqualTo(2);
        assertThat(overview.draftArticleCount()).isEqualTo(1);
        assertThat(overview.pendingReviewArticleCount()).isEqualTo(1);
        assertThat(overview.categorizedPublishedCount()).isEqualTo(1);
        assertThat(overview.taggedPublishedCount()).isEqualTo(1);
        assertThat(overview.collectionCount()).isEqualTo(1);
        assertThat(overview.collectionArticleCount()).isEqualTo(2);
        assertThat(overview.collectionOwnerCount()).isEqualTo(1);
    }

    @Test
    void exposesTheOverviewOnlyToAdministrators() throws Exception {
        AdminContentOverviewService service = new AdminContentOverviewService(() ->
                new ContentOperationsOverview(4, 2, 1, 1, 0, 1, 1, 1, 2, 1)
        );
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminContentOverviewController(service)).build();

        mvc.perform(get("/api/admin/content/overview").header("X-User-Roles", "AUTHOR"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/content/overview").header("X-User-Roles", "ADMIN,READER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedArticleCount").value(2))
                .andExpect(jsonPath("$.collectionArticleCount").value(2));
    }

    private static StoredArticle published(String id, Set<String> tags, String categoryId) {
        Article article = Article.draft(id, "u-author", id + " 标题");
        article.submitForPublish(ArticleVisibilityType.COMPANY, Set.of(), false);
        return stored(article, tags, categoryId);
    }

    private static StoredArticle stored(Article article, Set<String> tags, String categoryId) {
        return new StoredArticle(
                article,
                "{}",
                new ArticleContentProjection("<p>正文</p>", "正文"),
                tags,
                categoryId
        );
    }
}
