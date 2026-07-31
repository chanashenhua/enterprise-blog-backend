package com.company.blog.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.tag.api.CatalogItemRequest;
import com.company.blog.tag.api.CatalogService;
import com.company.blog.tag.api.JdbcCatalogRepository;
import com.company.blog.tag.api.JdbcTagAuditOutbox;
import com.company.blog.tag.api.TagAuditOutboxDispatcher;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TagAuditOutboxTest {
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:tag_audit_outbox;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void catalogMutationProducesAndDeliversAuditEvent() {
        CatalogService service = new CatalogService(
                new JdbcCatalogRepository(jdbcTemplate),
                new JdbcTagAuditOutbox(jdbcTemplate)
        );
        service.create(
                CatalogType.TAG,
                new CatalogItemRequest("observability", "可观测性"),
                "admin-1",
                List.of("ADMIN")
        );

        List<String> payloads = new ArrayList<>();
        new TagAuditOutboxDispatcher(jdbcTemplate, payloads::add, 20, 10).deliverPending();

        assertThat(payloads).singleElement().asString()
                .contains("\"actorId\":\"admin-1\"")
                .contains("\"action\":\"TAG_CREATE\"")
                .contains("\"resourceId\":\"observability\"");
        assertThat(jdbcTemplate.queryForObject(
                "select status from tag_audit_event",
                String.class
        )).isEqualTo("DELIVERED");
    }
}
