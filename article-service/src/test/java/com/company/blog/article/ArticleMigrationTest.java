package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(tableExists(jdbc, "DOMAIN_EVENT")).isTrue();
        assertThat(columnExists(jdbc, "ARTICLE", "VISIBILITY_TYPE")).isTrue();
        assertThat(columnExists(jdbc, "ARTICLE", "REVIEW_REQUEST_ID")).isTrue();
        assertThat(columnExists(jdbc, "ARTICLE", "CATEGORY_ID")).isTrue();
        assertThat(columnExists(jdbc, "ARTICLE_CONTENT_VERSION", "CATEGORY_ID")).isTrue();
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
