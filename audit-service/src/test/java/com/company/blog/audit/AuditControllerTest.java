package com.company.blog.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.audit.api.AuditApplicationService;
import com.company.blog.audit.api.AuditController;
import com.company.blog.audit.api.InternalAuditController;
import com.company.blog.audit.api.JdbcAuditRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuditControllerTest {
    private MockMvc mvc;
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:audit_controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        AuditApplicationService service = new AuditApplicationService(new JdbcAuditRepository(jdbcTemplate));
        mvc = MockMvcBuilders.standaloneSetup(
                new InternalAuditController(service, "test-audit-token"),
                new AuditController(service)
        ).build();
    }

    @Test
    void storesIdempotentEventsAndSupportsAdminFilters() throws Exception {
        String request = """
                {
                  "eventId":"event-1",
                  "sourceService":"review-service",
                  "actorId":"u-reviewer",
                  "actorRoles":["REVIEWER"],
                  "action":"REVIEW_APPROVE",
                  "resourceType":"ARTICLE",
                  "resourceId":"a-1",
                  "outcome":"SUCCESS",
                  "details":"ticket=t-1",
                  "traceId":"trace-1",
                  "occurredAt":"2026-08-01T04:00:00Z"
                }
                """;

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/internal/audits")
                            .header("X-Internal-Token", "test-audit-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventId").value("event-1"));
        }

        mvc.perform(get("/api/admin/audits")
                        .header("X-User-Roles", "ADMIN")
                        .param("actorId", "u-reviewer")
                        .param("action", "REVIEW_APPROVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resourceId").value("a-1"))
                .andExpect(jsonPath("$[0].actorRoles[0]").value("REVIEWER"));

        Long count = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from audit_record", Long.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void protectsInternalWriteAndAdminReadEndpoints() throws Exception {
        mvc.perform(post("/internal/audits")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/audits").header("X-User-Roles", "AUTHOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsAnInvalidTimeRange() throws Exception {
        mvc.perform(get("/api/admin/audits")
                        .header("X-User-Roles", "ADMIN")
                        .param("from", "2026-08-02T00:00:00Z")
                        .param("to", "2026-08-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }
}
