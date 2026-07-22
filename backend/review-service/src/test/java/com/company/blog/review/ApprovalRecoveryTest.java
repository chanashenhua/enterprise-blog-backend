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

class ApprovalRecoveryTest {
    @Test
    void uncertainApprovalCannotBeRejectedAndCanBeRetried() {
        InMemoryReviewTicketRepository repository = new InMemoryReviewTicketRepository();
        ReviewTicket ticket = repository.findOrCreateForArticle(ReviewTicketRepository.create(
                new CreateReviewTicketRequest("a-approval-retry", "rr-approval-retry", "u-author", "TEAM", Set.of("t-search"))
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

        assertThatThrownBy(() -> controller.reject(ticket.id(), new HttpHeaders()))
                .isInstanceOf(com.company.blog.review.api.ReviewTicketStateConflictException.class);

        controller.approve(ticket.id(), new HttpHeaders());

        assertThat(repository.findById(ticket.id()).orElseThrow().status()).isEqualTo(ReviewTicketStatus.APPROVED);
        assertThat(callback.publishedArticleIds).containsExactly("a-approval-retry");
    }

    private static final class ResponseLostAfterPublishCallback implements ArticlePublishCallbackClient {
        private final Set<String> publishedArticleIds = new HashSet<>();
        private boolean loseFirstResponse = true;

        @Override
        public void approveArticle(String articleId, String reviewTicketId) {
            boolean newlyPublished = publishedArticleIds.add(articleId);
            if (newlyPublished && loseFirstResponse) {
                loseFirstResponse = false;
                throw new RestClientException("response was lost after article-service committed");
            }
        }
    }
}
