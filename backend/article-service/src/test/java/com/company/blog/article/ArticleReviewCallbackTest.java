package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.article.api.ArticleController;
import com.company.blog.article.api.ArticleMemoryRepository;
import com.company.blog.article.api.ArticleOutbox;
import com.company.blog.article.api.ArticleService;
import com.company.blog.article.api.InternalArticleController;
import com.company.blog.article.api.CallerContext;
import com.company.blog.article.api.PermissionCheckClient;
import com.company.blog.article.api.ReviewPolicyClient;
import com.company.blog.article.api.ReviewTicketClient;
import com.company.blog.article.api.SubmitPublishRequest;
import com.company.blog.article.api.TagValidationClient;
import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ArticleReviewCallbackTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ArticleMemoryRepository repository = new ArticleMemoryRepository();
    private final RecordingOutbox outbox = new RecordingOutbox();
    private final RecordingReviewPolicyClient reviewPolicyClient = new RecordingReviewPolicyClient();
    private final ArticleService articleService = new ArticleService(
            repository,
            tagIds -> { },
            (callerContext, article, request) -> { },
            outbox,
            reviewPolicyClient,
            (article, request) -> { }
    );
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ArticleController(articleService), new InternalArticleController(articleService, "test-token")).build();

    @Test
    void reviewApproveCallbackPublishesPendingArticleAndEmitsEvent() throws Exception {
        reviewPolicyClient.nextReviewRequired = true;
        MvcResult draft = mvc.perform(post("/api/articles/drafts")
                        .header("X-User-Id", "u-author")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftRequest("Team Review")))
                .andExpect(status().isOk())
                .andReturn();
        String articleId = JsonTestValue.extractString(draft.getResponse().getContentAsString(), "id");

        mvc.perform(post("/api/articles/{articleId}/submit-publish", articleId)
                        .header("X-User-Id", "u-author")
                        .header("X-User-Roles", "AUTHOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(Map.of(
                                "visibilityType", "TEAM",
                                "targetOrgIds", List.of("t-search"),
                                "reviewRequired", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        String reviewRequestId = repository.findById(articleId).orElseThrow().article().reviewRequestId();

        mvc.perform(post("/internal/articles/{articleId}/review-approved", articleId)
                        .header("X-Internal-Token", "test-token")
                        .header("X-Review-Ticket-Id", "r-1")
                        .header("X-Review-Request-Id", reviewRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        assertThat(outbox.events).extracting(DomainEvent::type).containsExactly("ArticlePublished");
    }

    @Test
    void repeatedReviewApprovalCallbackIsIdempotent() {
        reviewPolicyClient.nextReviewRequired = true;
        String articleId = articleService.saveDraft("u-author", new com.company.blog.article.api.SaveDraftRequest(
                "Retry-safe approval",
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Retry-safe approval\"}]}]}",
                Set.of("redis")
        )).id();
        articleService.submitForPublish(
                articleId,
                new CallerContext("u-author", Set.of("AUTHOR"), Set.of(), Set.of("t-search")),
                new SubmitPublishRequest("TEAM", Set.of("t-search"), false)
        );
        String reviewRequestId = repository.findById(articleId).orElseThrow().article().reviewRequestId();

        articleService.approveFromReview(articleId, "r-1", reviewRequestId);
        articleService.approveFromReview(articleId, "r-1", reviewRequestId);

        assertThat(articleService.get(articleId).status()).isEqualTo("PUBLISHED");
        assertThat(outbox.events).extracting(DomainEvent::type).containsExactly("ArticlePublished");
    }

    private static String draftRequest(String title) throws Exception {
        String contentJson = OBJECT_MAPPER.writeValueAsString(Map.of(
                "type", "doc",
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", title))
                ))
        ));
        return OBJECT_MAPPER.writeValueAsString(Map.of(
                "title", title,
                "contentJson", contentJson,
                "tagIds", List.of("redis")
        ));
    }

    private static final class RecordingOutbox implements ArticleOutbox {
        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void appendArticleEvents(List<DomainEvent> events) {
            this.events.addAll(events);
        }
    }

    private static final class RecordingReviewPolicyClient implements ReviewPolicyClient {
        private boolean nextReviewRequired;

        @Override
        public boolean reviewRequired(SubmitPublishRequest request) {
            return nextReviewRequired;
        }
    }
}
