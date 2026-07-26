package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.article.api.ArticleController;
import com.company.blog.article.api.ArticleMemoryRepository;
import com.company.blog.article.api.ArticleOutbox;
import com.company.blog.article.api.ArticleService;
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

class ArticleControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ArticleMemoryRepository repository = new ArticleMemoryRepository();
    private final RecordingTagValidationClient tagValidationClient = new RecordingTagValidationClient();
    private final RecordingPermissionCheckClient permissionCheckClient = new RecordingPermissionCheckClient();
    private final RecordingArticleOutbox articleOutbox = new RecordingArticleOutbox();
    private final RecordingReviewPolicyClient reviewPolicyClient = new RecordingReviewPolicyClient();
    private final RecordingReviewTicketClient reviewTicketClient = new RecordingReviewTicketClient();
    private final ArticleService articleService = new ArticleService(
            repository,
            tagValidationClient,
            permissionCheckClient,
            articleOutbox,
            reviewPolicyClient,
            reviewTicketClient
    );
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ArticleController(articleService)).build();

    @Test
    void createsDraftPublishesAndFetchesArticle() throws Exception {
        String title = "Redis \u7f13\u5b58\u7b56\u7565";
        String contentJson = contentJson(title);
        String draftRequest = draftRequest(title, contentJson);

        MvcResult draft = mvc.perform(post("/api/articles/drafts")
                        .header("X-User-Id", "u-author")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.contentJson").value(contentJson))
                .andExpect(jsonPath("$.plainText").value(title))
                .andReturn();

        assertThat(tagValidationClient.validatedTagIds).containsExactly(Set.of("redis"));
        String articleId = JsonTestValue.extractString(draft.getResponse().getContentAsString(), "id");
        String publishRequest = OBJECT_MAPPER.writeValueAsString(Map.of(
                "visibilityType", "COMPANY",
                "targetOrgIds", List.of(),
                "reviewRequired", false
        ));

        mvc.perform(post("/api/articles/{articleId}/submit-publish", articleId)
                        .header("X-User-Id", "u-author")
                        .header("X-User-Roles", "AUTHOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        assertThat(permissionCheckClient.checkedPublishRequests).hasSize(1);
        assertThat(reviewPolicyClient.evaluatedRequests).hasSize(1);
        assertThat(articleOutbox.events).extracting(DomainEvent::type).containsExactly("ArticlePublished");
        assertThat(reviewTicketClient.ticketRequests).isEmpty();

        mvc.perform(get("/api/articles/{articleId}", articleId)
                        .header("X-User-Id", "u-reader")
                        .header("X-User-Roles", "READER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(articleId))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.contentJson").value(contentJson))
                .andExpect(jsonPath("$.renderedHtml").value("<p>" + title + "</p>"));
    }

    @Test
    void scopedArticleCreatesReviewTicketWhenReviewRequired() throws Exception {
        reviewPolicyClient.nextReviewRequired = true;
        String title = "Team Review";
        MvcResult draft = mvc.perform(post("/api/articles/drafts")
                        .header("X-User-Id", "u-author")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftRequest(title, contentJson(title))))
                .andExpect(status().isOk())
                .andReturn();
        String articleId = JsonTestValue.extractString(draft.getResponse().getContentAsString(), "id");
        String publishRequest = OBJECT_MAPPER.writeValueAsString(Map.of(
                "visibilityType", "TEAM",
                "targetOrgIds", List.of("t-search"),
                "reviewRequired", false
        ));

        mvc.perform(post("/api/articles/{articleId}/submit-publish", articleId)
                        .header("X-User-Id", "u-author")
                        .header("X-User-Roles", "AUTHOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        assertThat(articleOutbox.events).isEmpty();
        assertThat(reviewPolicyClient.evaluatedRequests).hasSize(1);
        assertThat(reviewTicketClient.ticketRequests).containsExactly(articleId);
    }

    private static String contentJson(String title) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(Map.of(
                "type", "doc",
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", title))
                ))
        ));
    }

    private static String draftRequest(String title, String contentJson) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(Map.of(
                "title", title,
                "contentJson", contentJson,
                "tagIds", List.of("redis")
        ));
    }

    private static final class RecordingTagValidationClient implements TagValidationClient {
        private final List<Set<String>> validatedTagIds = new ArrayList<>();

        @Override
        public void validate(Set<String> tagIds) {
            validatedTagIds.add(tagIds);
        }
    }

    private static final class RecordingPermissionCheckClient implements PermissionCheckClient {
        private final List<SubmitPublishRequest> checkedPublishRequests = new ArrayList<>();

        @Override
        public void requirePublishAllowed(CallerContext callerContext, Article article, SubmitPublishRequest request) {
            checkedPublishRequests.add(request);
        }
    }

    private static final class RecordingArticleOutbox implements ArticleOutbox {
        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void appendArticleEvents(List<DomainEvent> events) {
            this.events.addAll(events);
        }
    }

    private static final class RecordingReviewPolicyClient implements ReviewPolicyClient {
        private boolean nextReviewRequired;
        private final List<SubmitPublishRequest> evaluatedRequests = new ArrayList<>();

        @Override
        public boolean reviewRequired(SubmitPublishRequest request) {
            evaluatedRequests.add(request);
            return nextReviewRequired || request.reviewRequired();
        }
    }

    private static final class RecordingReviewTicketClient implements ReviewTicketClient {
        private final List<String> ticketRequests = new ArrayList<>();

        @Override
        public void createTicket(Article article, SubmitPublishRequest request) {
            ticketRequests.add(article.id());
        }
    }
}
