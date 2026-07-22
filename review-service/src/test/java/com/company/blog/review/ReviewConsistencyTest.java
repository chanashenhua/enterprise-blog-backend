package com.company.blog.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.review.api.ArticlePublishCallbackClient;
import com.company.blog.review.api.CreateReviewTicketRequest;
import com.company.blog.review.api.ReviewController;
import com.company.blog.review.api.ReviewTicketResponse;
import com.company.blog.review.api.ReviewTicketRepository;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;

class ReviewConsistencyTest {
    @Test
    void retryingTicketCreationReturnsExistingPendingTicketForSameArticle() {
        InMemoryReviewTicketRepository repository = new InMemoryReviewTicketRepository();
        CreateReviewTicketRequest request = new CreateReviewTicketRequest(
                "a-duplicate",
                "rr-duplicate",
                "u-author",
                "TEAM",
                Set.of("t-search")
        );

        ReviewController controller = new ReviewController(
                new ReviewPolicy(Set.of("t-search")),
                headers -> { },
                (articleId, ticketId) -> { },
                repository
        );

        ReviewTicketResponse first = controller.createTicket(request, "local-review-token");
        ReviewTicketResponse retried = controller.createTicket(request, "local-review-token");

        assertThat(retried.id()).isEqualTo(first.id());
        assertThat(retried.status()).isEqualTo("PENDING");
    }

    @Test
    void retryingApprovalConvergesAfterRemotePublishResponseIsLost() {
        InMemoryReviewTicketRepository repository = new InMemoryReviewTicketRepository();
        ReviewTicket ticket = repository.findOrCreateForArticle(ReviewTicketRepository.create(
                new CreateReviewTicketRequest("a-lost-response", "rr-lost-response", "u-author", "TEAM", Set.of("t-search"))
        ));
        ResponseLostAfterPublishCallback callback = new ResponseLostAfterPublishCallback();
        ReviewController controller = new ReviewController(
                new ReviewPolicy(Set.of("t-search")),
                headers -> { },
                callback,
                repository
        );

        assertThatThrownBy(() -> controller.approve(ticket.id(), new HttpHeaders()))
                .isInstanceOf(RestClientException.class);
        assertThat(repository.findById(ticket.id()).orElseThrow().status()).isEqualTo(ReviewTicketStatus.APPROVING);
        assertThat(callback.publishedArticleIds).containsExactly("a-lost-response");

        assertThatThrownBy(() -> controller.reject(ticket.id(), new HttpHeaders()))
                .isInstanceOf(com.company.blog.review.api.ReviewTicketStateConflictException.class);

        ReviewTicketResponse approved = controller.approve(ticket.id(), new HttpHeaders());

        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(repository.findById(ticket.id()).orElseThrow().status()).isEqualTo(ReviewTicketStatus.APPROVED);
        assertThat(callback.publishedArticleIds).containsExactly("a-lost-response");
        assertThat(callback.approvalAttemptTicketIds).containsExactly(ticket.id(), ticket.id());
    }

    private static final class ResponseLostAfterPublishCallback implements ArticlePublishCallbackClient {
        private final Set<String> publishedArticleIds = new HashSet<>();
        private final java.util.List<String> approvalAttemptTicketIds = new java.util.ArrayList<>();
        private boolean loseFirstResponse = true;

        @Override
        public void approveArticle(String articleId, String reviewTicketId) {
            approvalAttemptTicketIds.add(reviewTicketId);
            boolean newlyPublished = publishedArticleIds.add(articleId);
            if (newlyPublished && loseFirstResponse) {
                loseFirstResponse = false;
                throw new RestClientException("response was lost after article-service committed");
            }
        }
    }
}
