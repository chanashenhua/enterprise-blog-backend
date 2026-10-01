package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ArticleMigrationTest {
    @Test
    void createsArticleContentVisibilityPublishAndOutboxTables() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:article_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(tableExists(jdbc, "ARTICLE")).isTrue();
        assertThat(tableExists(jdbc, "ARTICLE_CONTENT")).isTrue();
        assertThat(tableExists(jdbc, "ARTICLE_VISIBILITY_TARGET")).isTrue();
        assertThat(tableExists(jdbc, "ARTICLE_PUBLISH_RECORD")).isTrue();
        assertThat(tableExists(jdbc, "ARTICLE_TAG")).isTrue();
        assertThat(tableExists(jdbc, "ARTICLE_CONTENT_VERSION")).isTrue();
        assertThat(tableExists(jdbc, "KNOWLEDGE_COLLECTION")).isTrue();
        assertThat(tableExists(jdbc, "KNOWLEDGE_COLLECTION_ARTICLE")).isTrue();
        assertThat(tableExists(jdbc, "DOMAIN_EVENT")).isTrue();
        assertThat(columnExists(jdbc, "ARTICLE", "VISIBILITY_TYPE")).isTrue();
        assertThat(columnExists(jdbc, "ARTICLE", "REVIEW_REQUEST_ID")).isTrue();
        assertThat(columnExists(jdbc, "ARTICLE", "CATEGORY_ID")).isTrue();
        assertThat(columnExists(jdbc, "ARTICLE", "REVISION")).isTrue();
        assertThat(columnExists(jdbc, "ARTICLE_CONTENT_VERSION", "CATEGORY_ID")).isTrue();
    }

    @Test
    void backfillsRevisionWithoutReplacingExistingContentOrVersions() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:article_revision_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("7").load().migrate();
        var jdbc = new JdbcTemplate(source);
        jdbc.update("insert into article (id, author_id, title, status) values ('legacy', 'author', '旧标题', 'DRAFT')");
        jdbc.update("insert into article_content (article_id, content_json, rendered_html, plain_text) values ('legacy', '旧源文', '<p>旧正文</p>', '旧正文')");
        jdbc.update("""
                insert into article_content_version
                (article_id, version_no, title, content_json, rendered_html, plain_text, tag_ids, created_by)
                values ('legacy', 7, '旧标题', '旧源文', '<p>旧正文</p>', '旧正文', '', 'author')
                """);
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        assertThat(jdbc.queryForObject("select revision from article where id = 'legacy'", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select content_json from article_content where article_id = 'legacy'", String.class)).isEqualTo("旧源文");
        assertThat(jdbc.queryForObject("select version_no from article_content_version where article_id = 'legacy'", Integer.class)).isEqualTo(7);
        assertThatThrownBy(() -> jdbc.update("update article set revision = 0 where id = 'legacy'"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private static boolean tableExists(JdbcTemplate jdbc, String tableName) {
        Integer count = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name = ?",
                Integer.class,
                tableName
        );
        return count != null && count == 1;
    }

    private static boolean columnExists(JdbcTemplate jdbc, String tableName, String columnName) {
        Integer count = jdbc.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where table_name = ? and column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count == 1;
    }
}
