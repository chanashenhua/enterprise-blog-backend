package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.article.api.ArticleMemoryRepository;
import com.company.blog.article.api.ArticleService;
import com.company.blog.article.api.CallerContext;
import com.company.blog.article.api.ReviewTicketClient;
import com.company.blog.article.api.SaveDraftRequest;
import com.company.blog.article.api.SubmitPublishRequest;
import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

class ArticleReviewTicketRecoveryTest {
    @Test
    void articleStaysPendingAndRetriesTicketCreationAfterResponseIsLost() {
        ResponseLostReviewTicketClient reviewTicketClient = new ResponseLostReviewTicketClient();
        ArticleService service = new ArticleService(
                new ArticleMemoryRepository(),
                tagIds -> { },
                (callerContext, article, request) -> { },
                events -> { },
                request -> true,
                reviewTicketClient
        );
        String articleId = service.saveDraft("u-author", new SaveDraftRequest(
                "Retry review ticket",
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Retry review ticket\"}]}]}",
                Set.of("redis")
        )).id();
        CallerContext author = new CallerContext("u-author", Set.of("AUTHOR"), Set.of(), Set.of("t-search"));
        SubmitPublishRequest request = new SubmitPublishRequest("TEAM", Set.of("t-search"), false);

        assertThatThrownBy(() -> service.submitForPublish(articleId, author, request))
                .isInstanceOf(RestClientException.class);
        assertThat(service.get(articleId).status()).isEqualTo(ArticleStatus.PENDING_REVIEW.name());

        assertThat(service.submitForPublish(articleId, author, request).status())
                .isEqualTo(ArticleStatus.PENDING_REVIEW.name());
        assertThat(reviewTicketClient.articleIds).containsExactly(articleId, articleId);
    }

    private static final class ResponseLostReviewTicketClient implements ReviewTicketClient {
        private final List<String> articleIds = new ArrayList<>();
        private boolean loseFirstResponse = true;

        @Override
        public void createTicket(Article article, SubmitPublishRequest request) {
            articleIds.add(article.id());
            if (loseFirstResponse) {
                loseFirstResponse = false;
                throw new RestClientException("review-service committed but response was lost");
            }
        }
    }
}
