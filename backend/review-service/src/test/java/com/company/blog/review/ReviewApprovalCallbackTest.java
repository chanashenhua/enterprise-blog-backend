package com.company.blog.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.review.api.ArticlePublishCallbackClient;
import com.company.blog.review.api.CreateReviewTicketRequest;
import com.company.blog.review.api.ReviewController;
import com.company.blog.review.api.ReviewExceptionHandler;
import com.company.blog.review.api.ReviewPermissionClient;
import com.company.blog.review.api.ReviewTicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReviewApprovalCallbackTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RecordingReviewPermissionClient permissionClient = new RecordingReviewPermissionClient();
    private final RecordingArticlePublishCallbackClient articleCallbackClient = new RecordingArticlePublishCallbackClient();
    private final InMemoryReviewTicketRepository repository = new InMemoryReviewTicketRepository();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new ReviewController(new ReviewPolicy(Set.of("t-search")), permissionClient, articleCallbackClient, repository)
    , new ReviewExceptionHandler()).build();

    @Test
    void approveTicketCallsArticlePublishCallback() throws Exception {
        MvcResult created = mvc.perform(post("/internal/reviews/tickets")
                        .header("X-Internal-Token", "local-review-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new CreateReviewTicketRequest(
                                "a-1",
                                "rr-a-1",
                                "u-author",
                                "team",
                                Set.of("t-search")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        String ticketId = OBJECT_MAPPER.readTree(created.getResponse().getContentAsString()).path("id").asText();

        mvc.perform(post("/api/admin/reviews/{ticketId}/approve", ticketId)
                        .header("X-User-Id", "u-reviewer")
                        .header("X-User-Roles", "REVIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertThat(articleCallbackClient.approvedArticleIds).containsExactly("a-1");
        assertThat(articleCallbackClient.approvedReviewRequestIds).containsExactly("rr-a-1");
    }

    private static final class RecordingReviewPermissionClient implements ReviewPermissionClient {
        @Override
        public void requireReviewAllowed(HttpHeaders headers) {
        }
    }

    private static final class RecordingArticlePublishCallbackClient implements ArticlePublishCallbackClient {
        private final List<String> approvedArticleIds = new ArrayList<>();
        private final List<String> approvedReviewRequestIds = new ArrayList<>();

        @Override
        public void approveArticle(String articleId, String reviewTicketId) {
            approvedArticleIds.add(articleId);
        }

        @Override
        public void approveArticle(String articleId, String reviewTicketId, String reviewRequestId) {
            approvedArticleIds.add(articleId);
            approvedReviewRequestIds.add(reviewRequestId);
        }
    }
}
