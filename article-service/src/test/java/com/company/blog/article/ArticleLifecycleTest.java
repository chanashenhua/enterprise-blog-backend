package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.article.api.ArticleMemoryRepository;
import com.company.blog.article.api.ArticleOutbox;
import com.company.blog.article.api.ArticleResponse;
import com.company.blog.article.api.ArticleService;
import com.company.blog.article.api.CallerContext;
import com.company.blog.article.api.SaveDraftRequest;
import com.company.blog.article.api.SubmitPublishRequest;
import com.company.blog.article.api.UpdateDraftRequest;
import com.company.blog.article.domain.ArticleStatus;
import com.company.blog.article.domain.DomainEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArticleLifecycleTest {
    private final ArticleMemoryRepository repository = new ArticleMemoryRepository();
    private final RecordingOutbox outbox = new RecordingOutbox();
    private final ArticleService service = new ArticleService(
            repository,
            tagIds -> { },
            (callerContext, article, request) -> { },
            outbox,
            request -> request.reviewRequired(),
            (article, request) -> { }
    );
    private final CallerContext author = new CallerContext(
            "u-author",
            Set.of("AUTHOR"),
            Set.of("d-platform"),
            Set.of("t-search")
    );

    @Test
    void editsListsWithdrawsRepublishesAndDeletesAnArticle() {
        String articleId = service.saveDraft(
                "u-author",
                new SaveDraftRequest("第一版", content("第一版正文"), Set.of("java"), "engineering")
        ).id();

        ArticleResponse updated = service.updateDraft(
                articleId,
                author,
                new UpdateDraftRequest("第二版", content("第二版正文"), Set.of("postgresql"), "database")
        );
        assertThat(updated.title()).isEqualTo("第二版");
        assertThat(updated.plainText()).isEqualTo("第二版正文");
        assertThat(updated.categoryId()).isEqualTo("database");
        assertThat(service.listMine(author)).extracting(ArticleResponse::id).containsExactly(articleId);
        assertThat(service.listVersions(articleId, author))
                .satisfies(versions -> {
                    assertThat(versions).extracting(version -> version.versionNo()).containsExactly(1, 2);
                    assertThat(versions).extracting(version -> version.categoryId())
                            .containsExactly("engineering", "database");
                });

        service.submitForPublish(
                articleId,
                author,
                new SubmitPublishRequest("COMPANY", Set.of(), false)
        );
        assertThat(service.withdraw(articleId, author).status()).isEqualTo(ArticleStatus.WITHDRAWN.name());

        ArticleResponse editedAfterWithdraw = service.updateDraft(
                articleId,
                author,
                new UpdateDraftRequest("第三版", content("第三版正文"), Set.of("java", "postgresql"))
        );
        assertThat(editedAfterWithdraw.status()).isEqualTo(ArticleStatus.DRAFT.name());
        assertThat(editedAfterWithdraw.visibilityType()).isNull();

        service.submitForPublish(
                articleId,
                author,
                new SubmitPublishRequest("COMPANY", Set.of(), false)
        );
        assertThat(service.delete(articleId, author).status()).isEqualTo(ArticleStatus.DELETED.name());
        assertThat(service.listMine(author)).isEmpty();
        assertThat(outbox.events).extracting(DomainEvent::type)
                .containsExactly("ArticlePublished", "ArticleWithdrawn", "ArticlePublished", "ArticleDeleted");
    }

    @Test
    void refusesToDeleteArticleWhileReviewIsPending() {
        String articleId = service.saveDraft(
                "u-author",
                new SaveDraftRequest("待审核", content("待审核正文"), Set.of())
        ).id();
        service.submitForPublish(
                articleId,
                author,
                new SubmitPublishRequest("TEAM", Set.of("t-search"), true)
        );

        assertThatThrownBy(() -> service.delete(articleId, author))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Pending review articles cannot be deleted");
    }

    private static String content(String text) {
        return """
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"%s"}]}]}
                """.formatted(text).trim();
    }

    private static final class RecordingOutbox implements ArticleOutbox {
        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void appendArticleEvents(List<DomainEvent> events) {
            this.events.addAll(events);
        }
    }
}
