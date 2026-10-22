package com.company.blog.article;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.company.blog.article.api.AdminSearchTaskController;
import com.company.blog.article.api.ArticleSearchOutboxDispatcher;
import com.company.blog.article.api.ArticleSearchSynchronizationService;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

class AdminSearchTaskControllerTest {
    private final JdbcTemplate jdbc = database();
    private final AdminSearchTaskController controller = new AdminSearchTaskController(jdbc);

    @Test
    void allSearchOperationsCanBeRetriedWithoutTouchingOtherOutboxes() {
        var types = List.of("ArticlePublished", "ArticleWithdrawn", "ArticleDeleted", "ArticleSearchSync");
        for (String type : types) insertEvent(type, type, "FAILED");
        insertEvent("other", "ArticleReviewRequested", "FAILED");
        assertThat(controller.list(admin(), "FAILED")).extracting(AdminSearchTaskController.SearchTaskResponse::eventType)
                .containsExactlyInAnyOrderElementsOf(types);
        for (String type : types) controller.retry(type, admin());
        assertThat(controller.list(admin(), "PENDING")).hasSize(4).allSatisfy(task -> assertThat(task.retryCount()).isZero());
        assertThat(jdbc.queryForObject("select status from domain_event where id = 'other'", String.class)).isEqualTo("FAILED");
        assertThatThrownBy(() -> controller.retry("other", admin())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller.retry("ArticlePublished", admin())).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void reconciliationQueuesAllStatesAndSkipsExistingPendingSyncs() {
        for (String state : List.of("DRAFT", "PUBLISHED", "DELETED")) {
            jdbc.update("insert into article (id, author_id, title, status, visibility_type) values (?, 'u-author', 'title', ?, 'COMPANY')", state, state);
        }
        assertThat(controller.reindex(admin()).queued()).isEqualTo(3);
        assertThat(controller.reindex(admin()).queued()).isZero();
        assertThat(controller.list(admin(), "PENDING")).extracting(AdminSearchTaskController.SearchTaskResponse::articleId)
                .containsExactlyInAnyOrder("DRAFT", "PUBLISHED", "DELETED");
    }

    @Test
    void repeatedDeliveryFailuresBecomeVisibleAndRetryResetsTheBudget() {
        insertEvent("failed", "ArticleWithdrawn", "PENDING");
        jdbc.update("update domain_event set retry_count = 0");
        var synchronization = mock(ArticleSearchSynchronizationService.class);
        doThrow(new IllegalStateException("offline")).when(synchronization).synchronize("a-1");
        var dispatcher = new ArticleSearchOutboxDispatcher(jdbc, synchronization, 20, 5);
        for (int i = 0; i < 5; i++) dispatcher.deliverPending();
        assertThat(controller.list(admin(), "FAILED")).singleElement().satisfies(task -> assertThat(task.retryCount()).isEqualTo(5));
        controller.retry("failed", admin());
        reset(synchronization);
        dispatcher.deliverPending();
        assertThat(jdbc.queryForObject("select status from domain_event where id = 'failed'", String.class)).isEqualTo("DELIVERED");
    }

    @Test
    void unauthenticatedAndNonAdministratorCannotReadRetryOrReindex() {
        HttpHeaders reader = new HttpHeaders(); reader.set("X-User-Id", "u-reader"); reader.set("X-User-Roles", "READER");
        HttpHeaders anonymous = new HttpHeaders(); anonymous.set("X-User-Roles", "ADMIN");
        for (HttpHeaders headers : List.of(reader, anonymous)) {
            assertThatThrownBy(() -> controller.list(headers, "FAILED")).isInstanceOf(ResponseStatusException.class);
            assertThatThrownBy(() -> controller.retry("any", headers)).isInstanceOf(ResponseStatusException.class);
            assertThatThrownBy(() -> controller.reindex(headers)).isInstanceOf(ResponseStatusException.class);
        }
        assertThatThrownBy(() -> controller.list(admin(), "DELIVERED")).isInstanceOf(ResponseStatusException.class);
        assertThat(jdbc.queryForObject("select count(*) from domain_event", Integer.class)).isZero();
    }

    private void insertEvent(String id, String type, String status) {
        jdbc.update("insert into domain_event (id, aggregate_type, aggregate_id, event_type, payload_json, status, retry_count) values (?, 'article', 'a-1', ?, '{}', ?, 5)", id, type, status);
    }

    private static HttpHeaders admin() {
        HttpHeaders headers = new HttpHeaders(); headers.set("X-User-Id", "u-admin"); headers.set("X-User-Roles", "READER, ADMIN"); return headers;
    }

    private static JdbcTemplate database() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:search_admin_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        return new JdbcTemplate(source);
    }
}
