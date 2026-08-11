package com.company.blog.stats;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.stats.api.ArticleInteractionController;
import com.company.blog.stats.api.ArticleInteractionResponse;
import com.company.blog.stats.api.ArticleInteractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ArticleInteractionControllerTest {
    private ArticleInteractionService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(ArticleInteractionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ArticleInteractionController(service)).build();
    }

    @Test
    void recordsAViewAndReturnsCurrentCounts() throws Exception {
        when(service.recordView(eq("a-1"), any(HttpHeaders.class)))
                .thenReturn(new ArticleInteractionResponse("a-1", 3, 2, 1, true, false));

        mvc.perform(post("/api/articles/a-1/interactions/views")
                        .header("X-User-Id", "u-reader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleId").value("a-1"))
                .andExpect(jsonPath("$.viewCount").value(3))
                .andExpect(jsonPath("$.liked").value(true));
    }

    @Test
    void supportsIdempotentLikeAndFavoriteRemovalEndpoints() throws Exception {
        when(service.like(eq("a-1"), any(HttpHeaders.class)))
                .thenReturn(new ArticleInteractionResponse("a-1", 1, 1, 0, true, false));
        when(service.unfavorite(eq("a-1"), any(HttpHeaders.class)))
                .thenReturn(new ArticleInteractionResponse("a-1", 1, 1, 0, true, false));

        mvc.perform(put("/api/articles/a-1/interactions/likes")
                        .header("X-User-Id", "u-reader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));
        mvc.perform(delete("/api/articles/a-1/interactions/favorites")
                        .header("X-User-Id", "u-reader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false));

        verify(service).like(eq("a-1"), any(HttpHeaders.class));
        verify(service).unfavorite(eq("a-1"), any(HttpHeaders.class));
    }
}
