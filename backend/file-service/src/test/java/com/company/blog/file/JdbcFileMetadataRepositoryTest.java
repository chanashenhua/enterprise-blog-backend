package com.company.blog.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.file.api.JdbcFileMetadataRepository;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcFileMetadataRepositoryTest {
    @Test
    void makesBindingRetriesIdempotentAndPersistsVerificationState() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:file_repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        JdbcFileMetadataRepository repository = new JdbcFileMetadataRepository(jdbcTemplate);
        repository.save(new FileMetadata(
                "f-1",
                "u-author",
                "files/u-author/f-1/diagram.png",
                "diagram.png",
                "image/png",
                1024,
                Instant.parse("2026-07-17T12:00:00Z")
        ));

        repository.bind("f-1", "ARTICLE", "a-1");
        repository.bind("f-1", "ARTICLE", "a-1");
        repository.markUploadVerified("f-1", "version-1");

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from file_binding where file_id = ? and resource_type = ? and resource_id = ?",
                Integer.class,
                "f-1",
                "ARTICLE",
                "a-1"
        )).isEqualTo(1);
        assertThat(repository.findById("f-1").orElseThrow().uploadVerified()).isTrue();
        assertThat(repository.findById("f-1").orElseThrow().verifiedVersionId()).isEqualTo("version-1");
    }
}
