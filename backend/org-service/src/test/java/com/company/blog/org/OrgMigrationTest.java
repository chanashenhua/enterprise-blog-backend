package com.company.blog.org;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OrgMigrationTest {
    @Test
    void seedsRequiredDepartmentsAndTeams() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:org_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("select count(*) from department", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "select department_id from team where id = 't-search'",
                String.class
        )).isEqualTo("d-platform");
        assertThat(jdbc.queryForObject(
                "select department_id from team where id = 't-pay'",
                String.class
        )).isEqualTo("d-pay");
    }
}
