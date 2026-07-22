package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.article.api.ArticleMemoryRepository;
import com.company.blog.article.api.ArticleService;
import com.company.blog.article.api.CallerContext;
import com.company.blog.article.api.SaveDraftRequest;
import com.company.blog.article.api.SubmitPublishRequest;
import com.company.blog.article.domain.ArticleStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArticleReviewRejectionTest {
    @Test
    void rejectionReturnsTheArticleToDraftAndAllowsAnotherReviewSubmission() {
        ArticleMemoryRepository repository = new ArticleMemoryRepository();
        ArticleService service = new ArticleService(
                repository,
                tagIds -> { },
                (callerContext, article, request) -> { },
                events -> { },
                request -> true,
                (article, request) -> { }
        );
        String articleId = service.saveDraft("u-author", new SaveDraftRequest(
                "Rejected review",
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Rejected review\"}]}]}",
                Set.of("redis")
        )).id();
        CallerContext author = new CallerContext("u-author", Set.of("AUTHOR"), Set.of(), Set.of("t-search"));
        SubmitPublishRequest request = new SubmitPublishRequest("TEAM", Set.of("t-search"), false);

        service.submitForPublish(articleId, author, request);
        assertThat(service.get(articleId).status()).isEqualTo(ArticleStatus.PENDING_REVIEW.name());
        String reviewRequestId = repository.findById(articleId).orElseThrow().article().reviewRequestId();

        service.rejectFromReview(articleId, "r-1", reviewRequestId);
        service.rejectFromReview(articleId, "r-1", reviewRequestId);

        assertThat(service.get(articleId).status()).isEqualTo(ArticleStatus.DRAFT.name());
        assertThat(service.get(articleId).visibilityType()).isNull();
        assertThat(service.submitForPublish(articleId, author, request).status())
                .isEqualTo(ArticleStatus.PENDING_REVIEW.name());
    }
}
