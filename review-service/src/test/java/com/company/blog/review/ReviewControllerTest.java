package com.company.blog.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.review.api.CreateReviewTicketRequest;
import com.company.blog.review.api.ReviewController;
import com.company.blog.review.api.ReviewExceptionHandler;
import com.company.blog.review.api.ReviewPermissionClient;
import com.company.blog.review.api.ReviewTicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReviewControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RecordingReviewPermissionClient permissionClient = new RecordingReviewPermissionClient();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new ReviewController(new ReviewPolicy(Set.of("t-search")), permissionClient, (articleId, ticketId) -> { }, new InMemoryReviewTicketRepository())
    , new ReviewExceptionHandler()).build();

    @Test
    void approveTicketRequiresReviewPermission() throws Exception {
        String createRequest = OBJECT_MAPPER.writeValueAsString(new CreateReviewTicketRequest(
                "a-1",
                "rr-a-1",
                "u-author",
                "team",
                Set.of("t-search")
        ));
        MvcResult created = mvc.perform(post("/internal/reviews/tickets")
                        .header("X-Internal-Token", "local-review-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        String ticketId = OBJECT_MAPPER.readTree(created.getResponse().getContentAsString()).path("id").asText();

        mvc.perform(post("/api/admin/reviews/{ticketId}/approve", ticketId)
                        .header("X-User-Id", "u-reviewer")
                        .header("X-User-Roles", "REVIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertThat(permissionClient.headers).hasSize(1);
        assertThat(permissionClient.headers.get(0).getFirst("X-User-Roles")).isEqualTo("REVIEWER");
    }

    @Test
    void internalApisRejectAnInvalidToken() throws Exception {
        String createRequest = OBJECT_MAPPER.writeValueAsString(new CreateReviewTicketRequest(
                "a-invalid-token",
                "rr-invalid-token",
                "u-author",
                "team",
                Set.of("t-search")
        ));

        mvc.perform(post("/internal/reviews/tickets")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isForbidden());

        mvc.perform(post("/internal/reviews/policies/evaluate")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(Map.of(
                                "visibilityType", "TEAM",
                                "targetOrgIds", List.of("t-search")
                        ))))
                .andExpect(status().isForbidden());
    }

    private static final class RecordingReviewPermissionClient implements ReviewPermissionClient {
        private final List<HttpHeaders> headers = new ArrayList<>();

        @Override
        public void requireReviewAllowed(HttpHeaders headers) {
            this.headers.add(headers);
        }
    }
}
