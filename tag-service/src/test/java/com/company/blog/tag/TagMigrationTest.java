package com.company.blog.tag;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TagMigrationTest {
    @Test
    void seedsCoreEngineeringTags() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:tag_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("select count(*) from tag", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select name from tag where id = 'elasticsearch'", String.class))
                .isEqualTo("Elasticsearch");
        assertThat(jdbc.queryForObject("select count(*) from category", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select active from tag where id = 'java'", Boolean.class)).isTrue();
    }
}
