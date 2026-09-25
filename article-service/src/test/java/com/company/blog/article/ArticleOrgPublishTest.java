package com.company.blog.article;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.company.blog.article.api.*;
import com.company.blog.article.domain.ArticleStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ArticleOrgPublishTest {
    private final ArticleMemoryRepository repository = new ArticleMemoryRepository();
    private final PermissionCheckClient permissions = mock(PermissionCheckClient.class);
    private final OrgValidationClient organizations = mock(OrgValidationClient.class);
    private final ArticleOutbox outbox = mock(ArticleOutbox.class);
    private final ReviewPolicyClient policy = request -> request.reviewRequired();
    private final ReviewTicketClient tickets = mock(ReviewTicketClient.class);
    private final CallerContext author = new CallerContext("u-author", Set.of("AUTHOR"), Set.of("d-platform"), Set.of("t-search"));
    private final ArticleService service = new ArticleService(repository, ids -> {}, permissions, outbox, policy, tickets, organizations);
    private String draft() {
        return service.saveDraft(author, new SaveDraftRequest("组织范围", "{\"type\":\"markdown\",\"version\":1,\"source\":\"正文\"}", Set.of(), null)).id();
    }

    @Test void validationFailuresDoNotChangeDraftOrCreateReviewOrOutbox() {
        for (HttpStatus status : new HttpStatus[]{HttpStatus.BAD_REQUEST, HttpStatus.SERVICE_UNAVAILABLE}) {
            String id = draft();
            doThrow(new ResponseStatusException(status, "Invalid org")).when(organizations).validate("TEAM", Set.of("t-search"));
            assertThatThrownBy(() -> service.submitForPublish(id, author, new SubmitPublishRequest("TEAM", Set.of("t-search"), true)))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getStatusCode()).isEqualTo(status));
            assertThat(repository.findById(id).orElseThrow().article().status()).isEqualTo(ArticleStatus.DRAFT);
        }
        verifyNoInteractions(tickets, outbox);
    }

    @Test void permissionDenialPrecedesOrganizationCallsAndWrites() {
        String id = draft();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(permissions).requirePublishAllowed(any(), any(), any());
        assertThatThrownBy(() -> service.submitForPublish(id, author, new SubmitPublishRequest("TEAM", Set.of("t-pay"), true)))
            .isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(repository.findById(id).orElseThrow().article().status()).isEqualTo(ArticleStatus.DRAFT);
        verifyNoInteractions(organizations, tickets, outbox);
    }

    @Test void validScopedTargetStillUsesExistingReviewWorkflow() {
        String id = draft();
        var result = service.submitForPublish(id, author, new SubmitPublishRequest("TEAM", Set.of("t-search"), true));
        assertThat(result.status()).isEqualTo("PENDING_REVIEW");
        var order = inOrder(permissions, organizations, tickets);
        order.verify(permissions).requirePublishAllowed(any(), any(), any());
        order.verify(organizations).validate("TEAM", Set.of("t-search"));
        order.verify(tickets).createTicket(any(), any());
        verifyNoInteractions(outbox);
    }

    @Test void malformedTargetsAndAnonymousRequestsAreRejectedBeforeStateChanges() {
        String id = draft();
        for (var request : new SubmitPublishRequest[]{new SubmitPublishRequest(null, Set.of(), false), new SubmitPublishRequest("TEAM", Set.of(), true), new SubmitPublishRequest("COMPANY", Set.of("t-search"), false)}) {
            assertThatThrownBy(() -> service.submitForPublish(id, author, request)).isInstanceOfSatisfying(ResponseStatusException.class,
                    ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        assertThatThrownBy(() -> service.submitForPublish(id, new CallerContext(null, Set.of(), Set.of(), Set.of()), new SubmitPublishRequest("COMPANY", Set.of(), false)))
            .isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(permissions, organizations, tickets, outbox);
    }
}
