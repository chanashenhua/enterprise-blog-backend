package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleStatus;
import com.company.blog.article.domain.ArticleVisibilityType;
import com.company.blog.article.domain.DomainEvent;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArticleStateMachineTest {
    @Test
    void companyArticlePublishesDirectly() {
        Article article = Article.draft("a-1", "u-author", "\u6807\u9898");
        article.submitForPublish(ArticleVisibilityType.COMPANY, Set.of(), false);

        assertThat(article.status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(article.pullEvents()).extracting(DomainEvent::type)
                .containsExactly("ArticlePublished");
    }

    @Test
    void companyArticlePublishesDirectlyEvenWhenReviewFlagIsTrue() {
        Article article = Article.draft("a-3", "u-author", "\u516c\u53f8\u6587\u7ae0");
        article.submitForPublish(ArticleVisibilityType.COMPANY, Set.of(), true);

        assertThat(article.status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(article.pullEvents()).extracting(DomainEvent::type)
                .containsExactly("ArticlePublished");
    }

    @Test
    void scopedArticleRequiresReviewWhenPolicyRequiresIt() {
        Article article = Article.draft("a-2", "u-author", "\u56e2\u961f\u6587\u7ae0");
        article.submitForPublish(ArticleVisibilityType.TEAM, Set.of("t-search"), true);

        assertThat(article.status()).isEqualTo(ArticleStatus.PENDING_REVIEW);
        assertThat(article.pullEvents()).isEmpty();
    }

    @Test
    void pendingReviewRequestCanBeRetriedForTheSameScope() {
        Article article = Article.draft("a-4", "u-author", "Retry review");

        article.requestReview(ArticleVisibilityType.TEAM, Set.of("t-search"));
        String reviewRequestId = article.reviewRequestId();
        article.requestReview(ArticleVisibilityType.TEAM, Set.of("t-search"));

        assertThat(article.status()).isEqualTo(ArticleStatus.PENDING_REVIEW);
        assertThat(article.visibilityType()).isEqualTo(ArticleVisibilityType.TEAM);
        assertThat(article.visibilityTargetIds()).containsExactly("t-search");
        assertThat(article.reviewRequestId()).isEqualTo(reviewRequestId);
    }

    @Test
    void staleRejectionCallbackCannotAffectTheNextReviewRound() {
        Article article = Article.draft("a-5", "u-author", "Review rounds");
        article.requestReview(ArticleVisibilityType.TEAM, Set.of("t-search"));
        String firstRequestId = article.reviewRequestId();

        article.rejectFromReview("r-first", firstRequestId);
        article.requestReview(ArticleVisibilityType.TEAM, Set.of("t-search"));
        String secondRequestId = article.reviewRequestId();

        article.rejectFromReview("r-first", firstRequestId);

        assertThat(secondRequestId).isNotEqualTo(firstRequestId);
        assertThat(article.status()).isEqualTo(ArticleStatus.PENDING_REVIEW);
        assertThat(article.reviewRequestId()).isEqualTo(secondRequestId);
    }
}
