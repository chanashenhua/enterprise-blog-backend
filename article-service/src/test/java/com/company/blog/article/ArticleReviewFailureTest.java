package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.article.api.ArticleMemoryRepository;
import com.company.blog.article.api.ArticleOutbox;
import com.company.blog.article.api.ArticleService;
import com.company.blog.article.api.ReviewPolicyClient;
import com.company.blog.article.api.SubmitPublishRequest;
import com.company.blog.article.domain.ArticleStatus;
import com.company.blog.article.domain.DomainEvent;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArticleReviewFailureTest {
    @Test
    void failedReviewTicketCreationLeavesArticlePendingForRetry() {
        ArticleMemoryRepository repository = new ArticleMemoryRepository();
        ArticleService service = new ArticleService(
                repository,
                tagIds -> { },
                (callerContext, article, request) -> { },
                events -> { },
                request -> true,
                (article, request) -> { throw new IllegalStateException("review-service down"); }
        );
        String articleId = service.saveDraft("u-author", new com.company.blog.article.api.SaveDraftRequest(
                "Team Review",
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Team Review\"}]}]}",
                Set.of("redis")
        )).id();

        SubmitPublishRequest request = new SubmitPublishRequest("TEAM", Set.of("t-search"), false);

        assertThatThrownBy(() -> service.submitForPublish(articleId, new com.company.blog.article.api.CallerContext(
                "u-author", Set.of("AUTHOR"), Set.of(), Set.of("t-search")
        ), request)).isInstanceOf(IllegalStateException.class);

        assertThat(service.get(articleId).status()).isEqualTo(ArticleStatus.PENDING_REVIEW.name());
        assertThat(service.get(articleId).visibilityType()).isEqualTo("TEAM");
        assertThat(service.get(articleId).visibilityTargetIds()).containsExactly("t-search");
    }
}
