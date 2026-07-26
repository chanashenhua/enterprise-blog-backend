package com.company.blog.tag;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.tag.api.AdminTagController;
import com.company.blog.tag.api.CatalogController;
import com.company.blog.tag.api.CatalogService;
import com.company.blog.tag.api.JdbcCatalogRepository;
import com.company.blog.tag.api.TagController;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TagControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:tag_controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .clean();
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        CatalogService service = new CatalogService(new JdbcCatalogRepository(new JdbcTemplate(dataSource)));
        mvc = MockMvcBuilders.standaloneSetup(
                new TagController(service),
                new CatalogController(service),
                new AdminTagController(service)
        ).build();
    }

    @Test
    void validatesKnownEngineeringTags() throws Exception {
        mvc.perform(get("/internal/tags/validate")
                        .param("ids", "java")
                        .param("ids", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.validIds[0]").value("java"))
                .andExpect(jsonPath("$.unknownIds[0]").value("missing"));
    }

    @Test
    void adminCreatesAndDeactivatesManagedCategories() throws Exception {
        mvc.perform(post("/api/admin/categories")
                        .header("X-User-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"security\",\"name\":\"安全实践\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("security"))
                .andExpect(jsonPath("$.active").value(true));

        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'security')]").exists());
    }

    @Test
    void rejectsCatalogManagementWithoutAdminRole() throws Exception {
        mvc.perform(post("/api/admin/tags")
                        .header("X-User-Roles", "AUTHOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"kafka\",\"name\":\"Kafka\"}"))
                .andExpect(status().isForbidden());
    }
}
