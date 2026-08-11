package com.company.blog.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.stats.api.JdbcInteractionRepository;
import com.company.blog.stats.api.JdbcPersonalInteractionRepository;
import com.company.blog.stats.api.PersonalInteractionItem;
import com.company.blog.stats.api.PersonalInteractionRepository;
import com.company.blog.stats.api.PersonalKnowledgeController;
import com.company.blog.stats.api.PersonalKnowledgeService;
import java.time.Instant;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PersonalKnowledgeTest {
    @Test
    void returnsCurrentFavoritesAndRecentViews() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:personal_knowledge;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        JdbcInteractionRepository interactions = new JdbcInteractionRepository(jdbcTemplate);
        interactions.add("a-1", "u-reader", InteractionType.VIEW);
        interactions.add("a-2", "u-reader", InteractionType.VIEW);
        interactions.add("a-2", "u-reader", InteractionType.FAVORITE);
        interactions.add("a-3", "u-author", InteractionType.FAVORITE);

        JdbcPersonalInteractionRepository repository = new JdbcPersonalInteractionRepository(jdbcTemplate);

        assertThat(repository.findByUser("u-reader", InteractionType.FAVORITE, 20))
                .extracting(PersonalInteractionItem::articleId)
                .containsExactly("a-2");
        assertThat(repository.findByUser("u-reader", InteractionType.VIEW, 20))
                .extracting(PersonalInteractionItem::articleId)
                .containsExactlyInAnyOrder("a-1", "a-2");
    }

    @Test
    void endpointsRequireIdentityAndClampLimit() throws Exception {
        PersonalInteractionRepository repository = mock(PersonalInteractionRepository.class);
        when(repository.findByUser("u-reader", InteractionType.FAVORITE, 100)).thenReturn(List.of(
                new PersonalInteractionItem("a-1", "FAVORITE", Instant.parse("2026-08-01T00:00:00Z"))
        ));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new PersonalKnowledgeController(new PersonalKnowledgeService(repository))
        ).build();

        mvc.perform(get("/api/me/knowledge/favorites"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/me/knowledge/favorites")
                        .header("X-User-Id", "u-reader")
                        .param("limit", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].articleId").value("a-1"));

        verify(repository).findByUser("u-reader", InteractionType.FAVORITE, 100);
    }
}
