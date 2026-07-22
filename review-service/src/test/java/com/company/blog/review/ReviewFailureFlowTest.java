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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClientException;

class ReviewFailureFlowTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ReviewTicketRepository repository = new InMemoryReviewTicketRepository();
    private final FailingArticleCallbackClient callbackClient = new FailingArticleCallbackClient();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new ReviewController(new ReviewPolicy(Set.of("t-search")), headers -> { }, callbackClient, repository)
    , new ReviewExceptionHandler()).build();

    @Test
    void approveUnknownTicketReturnsNotFound() throws Exception {
        mvc.perform(post("/api/admin/reviews/{ticketId}/approve", "missing")
                        .header("X-User-Id", "u-reviewer")
                        .header("X-User-Roles", "REVIEWER"))
                .andExpect(status().isNotFound());
    }

    @Test
    void callbackFailureLeavesTicketApprovingAndCannotBeRejected() throws Exception {
        String ticketId = createTicket("a-1");
        callbackClient.fail = true;

        mvc.perform(post("/api/admin/reviews/{ticketId}/approve", ticketId)
                        .header("X-User-Id", "u-reviewer")
                        .header("X-User-Roles", "REVIEWER"))
                .andExpect(status().isBadGateway());

        assertThat(repository.findById(ticketId).orElseThrow().status()).isEqualTo(ReviewTicketStatus.APPROVING);

        mvc.perform(post("/api/admin/reviews/{ticketId}/reject", ticketId)
                        .header("X-User-Id", "u-reviewer")
                        .header("X-User-Roles", "REVIEWER"))
                .andExpect(status().isConflict());
    }

    @Test
    void approvedTicketCannotBeRejectedAgain() throws Exception {
        String ticketId = createTicket("a-2");
        mvc.perform(post("/api/admin/reviews/{ticketId}/approve", ticketId)
                        .header("X-User-Id", "u-reviewer")
                        .header("X-User-Roles", "REVIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(post("/api/admin/reviews/{ticketId}/reject", ticketId)
                        .header("X-User-Id", "u-reviewer")
                        .header("X-User-Roles", "REVIEWER"))
                .andExpect(status().isConflict());
    }

    @Test
    void approvedTicketCannotBeApprovedAgainAndDoesNotInvokeCallback() throws Exception {
        String ticketId = createTicket("a-3");
        mvc.perform(post("/api/admin/reviews/{ticketId}/approve", ticketId)
                        .header("X-User-Id", "u-reviewer")
                        .header("X-User-Roles", "REVIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertThat(callbackClient.approvedArticleIds).containsExactly("a-3");

        mvc.perform(post("/api/admin/reviews/{ticketId}/approve", ticketId)
                        .header("X-User-Id", "u-reviewer")
                        .header("X-User-Roles", "REVIEWER"))
                .andExpect(status().isConflict());

        assertThat(callbackClient.approvedArticleIds).containsExactly("a-3");
    }

    private String createTicket(String articleId) throws Exception {
        MvcResult created = mvc.perform(post("/internal/reviews/tickets")
                        .header("X-Internal-Token", "local-review-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new CreateReviewTicketRequest(
                                articleId,
                                "rr-" + articleId,
                                "u-author",
                                "team",
                                Set.of("t-search")
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return OBJECT_MAPPER.readTree(created.getResponse().getContentAsString()).path("id").asText();
    }

    private static final class FailingArticleCallbackClient implements ArticlePublishCallbackClient {
        private boolean fail;
        private final java.util.List<String> approvedArticleIds = new java.util.ArrayList<>();

        @Override
        public void approveArticle(String articleId, String reviewTicketId) {
            approvedArticleIds.add(articleId);
            if (fail) {
                throw new RestClientException("article-service down");
            }
        }
    }
}
