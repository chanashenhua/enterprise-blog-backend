package com.company.blog.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class UserMigrationTest {
    @Test
    void seedsRequiredDemoUsersRolesAndMemberships() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:user_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("select count(*) from blog_user", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from user_role where user_id = 'u-admin'", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject(
                "select team_id from user_org_membership where user_id = 'u-author'",
                String.class
        )).isEqualTo("t-search");
    }
}
