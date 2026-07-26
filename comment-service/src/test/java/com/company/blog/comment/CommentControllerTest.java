package com.company.blog.comment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.comment.api.CommentController;
import com.company.blog.comment.api.CommentResponse;
import com.company.blog.comment.api.CommentService;
import com.company.blog.comment.api.CreateCommentRequest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CommentControllerTest {
    private CommentService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(CommentService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CommentController(service)).build();
    }

    @Test
    void createsACommentUsingGatewayIdentityHeaders() throws Exception {
        Instant now = Instant.parse("2026-07-25T12:00:00Z");
        when(service.create(eq("a-1"), any(CreateCommentRequest.class), any(HttpHeaders.class)))
                .thenReturn(new CommentResponse(
                        "c-1",
                        "a-1",
                        null,
                        "u-author",
                        "Hello",
                        false,
                        now,
                        now
                ));

        mvc.perform(post("/api/articles/a-1/comments")
                        .header("X-User-Id", "u-author")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("c-1"))
                .andExpect(jsonPath("$.content").value("Hello"));
    }

    @Test
    void deletesThroughTheSoftDeleteServiceOperation() throws Exception {
        mvc.perform(delete("/api/articles/a-1/comments/c-1")
                        .header("X-User-Id", "u-admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isNoContent());

        verify(service).delete(eq("a-1"), eq("c-1"), any(HttpHeaders.class));
    }
}
