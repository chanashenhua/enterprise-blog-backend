package com.company.blog.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.stats.api.AdminInteractionController;
import com.company.blog.stats.api.AdminInteractionOverview;
import com.company.blog.stats.api.AdminInteractionRepository;
import com.company.blog.stats.api.AdminInteractionService;
import com.company.blog.stats.api.JdbcAdminInteractionRepository;
import com.company.blog.stats.api.JdbcInteractionRepository;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminInteractionOverviewTest {
    @Test
    void aggregatesTotalsAndRanksArticles() {
        DriverManagerDataSource dataSource = dataSource("stats_admin_repository");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        JdbcInteractionRepository interactions = new JdbcInteractionRepository(jdbcTemplate);
        interactions.add("a-popular", "u-1", InteractionType.VIEW);
        interactions.add("a-popular", "u-2", InteractionType.VIEW);
        interactions.add("a-popular", "u-1", InteractionType.LIKE);
        interactions.add("a-popular", "u-2", InteractionType.FAVORITE);
        interactions.add("a-second", "u-1", InteractionType.VIEW);

        AdminInteractionOverview overview = new JdbcAdminInteractionRepository(jdbcTemplate).overview(10);

        assertThat(overview.viewCount()).isEqualTo(3);
        assertThat(overview.likeCount()).isEqualTo(1);
        assertThat(overview.favoriteCount()).isEqualTo(1);
        assertThat(overview.activeArticleCount()).isEqualTo(2);
        assertThat(overview.engagedUserCount()).isEqualTo(2);
        assertThat(overview.topArticles()).extracting(item -> item.articleId())
                .containsExactly("a-popular", "a-second");
        assertThat(overview.topArticles().get(0).engagementCount()).isEqualTo(2);
    }

    @Test
    void adminEndpointRequiresAdminRoleAndClampsLimit() throws Exception {
        AdminInteractionRepository repository = mock(AdminInteractionRepository.class);
        when(repository.overview(50)).thenReturn(new AdminInteractionOverview(3, 1, 1, 2, 2, List.of()));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new AdminInteractionController(new AdminInteractionService(repository))
        ).build();

        mvc.perform(get("/api/admin/stats/overview").header("X-User-Roles", "AUTHOR"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/stats/overview")
                        .header("X-User-Roles", "ADMIN")
                        .param("limit", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(3));
    }

    private static DriverManagerDataSource dataSource(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        return dataSource;
    }
}
