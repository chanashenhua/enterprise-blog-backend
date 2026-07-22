package com.company.blog.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class FileMigrationTest {
    @Test
    void createsFileMetadataAndBindingTables() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:file_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Instant createdAt = Instant.parse("2026-07-17T12:00:00Z");

        jdbcTemplate.update(
                """
                        insert into file_metadata
                            (id, owner_id, object_key, original_name, content_type, size_bytes, created_at)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                "f-1",
                "u-author",
                "files/u-author/f-1/diagram.png",
                "diagram.png",
                "image/png",
                1024,
                Timestamp.from(createdAt)
        );
        jdbcTemplate.update(
                "insert into file_binding (file_id, resource_type, resource_id) values (?, ?, ?)",
                "f-1",
                "ARTICLE",
                "a-1"
        );

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from file_metadata where owner_id = ? and content_type = ?",
                Integer.class,
                "u-author",
                "image/png"
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from file_binding where file_id = ? and resource_id = ?",
                Integer.class,
                "f-1",
                "a-1"
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select upload_verified from file_metadata where id = ?",
                Boolean.class,
                "f-1"
        )).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "select verified_version_id from file_metadata where id = ?",
                String.class,
                "f-1"
        )).isNull();
    }

    @Test
    void resetsLegacyVerificationFlagsUntilAnObjectVersionIsPinned() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:file_migration_v2;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("2")
                .load()
                .migrate();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                """
                        insert into file_metadata
                            (id, owner_id, object_key, original_name, content_type, size_bytes, created_at, upload_verified)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                "f-v2",
                "u-author",
                "files/u-author/f-v2/diagram.png",
                "diagram.png",
                "image/png",
                1024,
                Timestamp.from(Instant.parse("2026-07-17T12:00:00Z")),
                true
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(jdbcTemplate.queryForObject(
                "select upload_verified from file_metadata where id = ?",
                Boolean.class,
                "f-v2"
        )).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "select verified_version_id from file_metadata where id = ?",
                String.class,
                "f-v2"
        )).isNull();
    }
}
