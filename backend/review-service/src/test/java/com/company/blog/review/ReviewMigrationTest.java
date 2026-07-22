package com.company.blog.review;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ReviewMigrationTest {
    @Test
    void createsReviewTicketTable() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:review_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer count = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'REVIEW_TICKET'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
        Integer reviewRequestIdColumnCount = jdbc.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where table_name = 'REVIEW_TICKET' and column_name = 'REVIEW_REQUEST_ID'
                        """,
                Integer.class
        );
        assertThat(reviewRequestIdColumnCount).isEqualTo(1);
    }
}
