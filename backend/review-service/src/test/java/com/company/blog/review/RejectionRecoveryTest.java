package com.company.blog.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.review.api.ArticlePublishCallbackClient;
import com.company.blog.review.api.CreateReviewTicketRequest;
import com.company.blog.review.api.ReviewController;
import com.company.blog.review.api.ReviewTicketRepository;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;

class RejectionRecoveryTest {
    @Test
    void uncertainRejectionCannotBeApprovedAndCanBeRetried() {
        InMemoryReviewTicketRepository repository = new InMemoryReviewTicketRepository();
        ReviewTicket ticket = repository.findOrCreateForArticle(ReviewTicketRepository.create(
                new CreateReviewTicketRequest("a-rejection-retry", "rr-rejection-retry", "u-author", "TEAM", Set.of("t-search"))
        ));
        ResponseLostAfterRejectionCallback callback = new ResponseLostAfterRejectionCallback();
        ReviewController controller = new ReviewController(
                new ReviewPolicy(Set.of("t-search")),
                headers -> { },
                callback,
                repository
        );

        assertThatThrownBy(() -> controller.reject(ticket.id(), new HttpHeaders()))
                .isInstanceOf(RestClientException.class);
        assertThat(repository.findById(ticket.id()).orElseThrow().status()).isEqualTo(ReviewTicketStatus.REJECTING);

        assertThatThrownBy(() -> controller.approve(ticket.id(), new HttpHeaders()))
                .isInstanceOf(com.company.blog.review.api.ReviewTicketStateConflictException.class);

        controller.reject(ticket.id(), new HttpHeaders());

        assertThat(repository.findById(ticket.id()).orElseThrow().status()).isEqualTo(ReviewTicketStatus.REJECTED);
        assertThat(callback.rejectedArticleIds).containsExactly("a-rejection-retry");
    }

    private static final class ResponseLostAfterRejectionCallback implements ArticlePublishCallbackClient {
        private final Set<String> rejectedArticleIds = new HashSet<>();
        private boolean loseFirstResponse = true;

        @Override
        public void approveArticle(String articleId, String reviewTicketId) {
        }

        @Override
        public void rejectArticle(String articleId, String reviewTicketId) {
            boolean newlyRejected = rejectedArticleIds.add(articleId);
            if (newlyRejected && loseFirstResponse) {
                loseFirstResponse = false;
                throw new RestClientException("response was lost after article-service committed");
            }
        }
    }
}
