package com.company.blog.article;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.company.blog.article.api.*;
import com.company.blog.article.domain.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ArticleSearchSynchronizationTest {
    private final ArticleRepository repository = mock(ArticleRepository.class);
    private final ArticleSearchIndexClient client = mock(ArticleSearchIndexClient.class);
    private final PermissionCheckClient permissions = mock(PermissionCheckClient.class);

    private StoredArticle article(long revision) {
        Article article = Article.draft("a-1", "u-author", "最新标题");
        article.submitForPublish(ArticleVisibilityType.TEAM, Set.of("t-search"), false);
        return new StoredArticle(article, "{}", new ArticleContentProjection("<p>当前正文</p>", "当前正文"),
                Set.of("java"), "engineering", revision);
    }

    @Test
    void retrySynchronizesLatestContentAndRevisionInsteadOfEventSnapshot() {
        when(repository.findByIdForUpdate("a-1")).thenReturn(Optional.of(article(8)));
        new ArticleSearchSynchronizationService(repository, client).synchronize("a-1");
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(client).index(payload.capture());
        assertThat(payload.getValue()).contains("\"revision\":8", "最新标题", "\"categoryId\":\"engineering\"");
        verify(client, never()).delete(anyString(), anyLong());
    }

    @Test
    void retriedPublishCannotResurrectWithdrawnArticle() {
        StoredArticle withdrawn = article(9);
        withdrawn.article().withdraw();
        when(repository.findByIdForUpdate("a-1")).thenReturn(Optional.of(withdrawn));
        new ArticleSearchSynchronizationService(repository, client).synchronize("a-1");
        verify(client).delete("a-1", 9);
        verify(client, never()).index(anyString());
    }

    @Test
    void currentDatabaseStatusRevisionAndPermissionAllHaveToMatch() {
        StoredArticle current = article(8);
        when(repository.findById("a-1")).thenReturn(Optional.of(current));
        var controller = new ArticleSearchAccessController(repository, permissions, "test-token");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "u-reader");
        assertThat(controller.readableIds("test-token", headers, List.of(new ArticleSearchAccessController.Candidate("a-1", 7)))).isEmpty();
        verifyNoInteractions(permissions);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(permissions).requireReadAllowed(any(), any());
        assertThat(controller.readableIds("test-token", headers, List.of(new ArticleSearchAccessController.Candidate("a-1", 8)))).isEmpty();
        reset(permissions);
        assertThat(controller.readableIds("test-token", headers, List.of(new ArticleSearchAccessController.Candidate("a-1", 8)))).containsExactly("a-1");
        current.article().withdraw();
        assertThat(controller.readableIds("test-token", headers, List.of(new ArticleSearchAccessController.Candidate("a-1", 8)))).isEmpty();
    }

    @Test
    void rejectsPublicAccessAndFailsClosedWhenPermissionServiceFails() {
        when(repository.findById("a-1")).thenReturn(Optional.of(article(8)));
        var controller = new ArticleSearchAccessController(repository, permissions, "test-token");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "u-reader");
        var candidates = List.of(new ArticleSearchAccessController.Candidate("a-1", 8));
        assertThatThrownBy(() -> controller.readableIds("wrong", headers, candidates)).isInstanceOf(ResponseStatusException.class);
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(permissions).requireReadAllowed(any(), any());
        assertThatThrownBy(() -> controller.readableIds("test-token", headers, candidates)).isInstanceOf(ResponseStatusException.class);
    }
}
