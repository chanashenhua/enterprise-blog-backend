package com.company.blog.org;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OrgDirectoryControllerTest {
    private JdbcTemplate jdbc;
    private JdbcOrgRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:org_directory_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
        repository = new JdbcOrgRepository(jdbc);
        mvc = MockMvcBuilders.standaloneSetup(new OrgDirectoryController(repository, "test-token")).build();
    }

    @Test
    void returnsOnlyAuthorsExistingMembershipsWithDatabaseNames() throws Exception {
        jdbc.update("update department set name = ? where id = ?", "平台工程部", "d-platform");
        mvc.perform(get("/api/organizations/publish-options").header("X-User-Id", "author").header("X-User-Roles", "AUTHOR,READER")
                .header("X-Department-Ids", "d-platform,missing").header("X-Team-Ids", "t-search"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.departments.length()").value(1))
            .andExpect(jsonPath("$.departments[0].name").value("平台工程部"))
            .andExpect(jsonPath("$.teams.length()").value(1)).andExpect(jsonPath("$.teams[0].id").value("t-search"))
            .andExpect(jsonPath("$.teams[0].departmentName").value("平台工程部"));
    }

    @Test
    void adminCanChooseAllOrganizationsButEmptyAuthorClaimsDoNotExpandScope() throws Exception {
        mvc.perform(get("/api/organizations/publish-options").header("X-User-Id", "admin").header("X-User-Roles", "ADMIN"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.departments.length()").value(2)).andExpect(jsonPath("$.teams.length()").value(2));
        mvc.perform(get("/api/organizations/publish-options?roles=ADMIN&teamIds=t-pay").header("X-User-Id", "author").header("X-User-Roles", "AUTHOR"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.departments").isEmpty()).andExpect(jsonPath("$.teams").isEmpty());
    }

    @Test
    void deniesAnonymousAndReadersBeforeReadingDirectory() throws Exception {
        mvc.perform(get("/api/organizations/publish-options")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/organizations/publish-options").header("X-User-Id", "reader").header("X-User-Roles", "READER"))
            .andExpect(status().isForbidden());
    }

    @Test
    void validatesAllIdsAndExactOrganizationTypeAndDetectsDeletion() throws Exception {
        validate("TEAM", Set.of("t-search"), true);
        validate("DEPARTMENT", Set.of("d-platform", "d-pay"), true);
        validate("TEAM", Set.of("t-search", "missing"), false);
        validate("TEAM", Set.of("d-platform"), false);
        jdbc.update("delete from team where id = ?", "t-search");
        validate("TEAM", Set.of("t-search"), false);
        assertThat(repository.directory(false, Set.of("d-platform"), Set.of("t-search")).teams()).isEmpty();
    }

    @Test
    void internalValidationRequiresTokenAndValidShape() throws Exception {
        String payload = payload("TEAM", Set.of("t-search"));
        mvc.perform(post("/internal/organizations/validate-targets").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isForbidden());
        var unconfigured = MockMvcBuilders.standaloneSetup(new OrgDirectoryController(repository, "")).build();
        unconfigured.perform(post("/internal/organizations/validate-targets").header("X-Internal-Org-Token", "test-token")
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        for (String body : new String[]{payload("TEAM", Set.of()), payload("COMPANY", Set.of("t-search")), payload("OTHER", Set.of("t-search")), payload("TEAM", Set.of(" "))}) {
            mvc.perform(post("/internal/organizations/validate-targets").header("X-Internal-Org-Token", "test-token")
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
    }

    private void validate(String type, Set<String> ids, boolean expected) throws Exception {
        mvc.perform(post("/internal/organizations/validate-targets").header("X-Internal-Org-Token", "test-token")
                .contentType(MediaType.APPLICATION_JSON).content(payload(type, ids)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(expected));
    }
    private String payload(String type, Set<String> ids) throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of("visibilityType", type, "targetOrgIds", ids));
    }
}
