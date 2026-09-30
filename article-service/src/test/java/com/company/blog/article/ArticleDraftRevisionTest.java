package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.company.blog.article.api.*;
import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import com.company.blog.article.domain.ArticleStatus;
import com.company.blog.article.domain.ArticleVisibilityType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.server.ResponseStatusException;

/** Uses real JDBC transactions and separate connections for competing editor requests. */
@SpringJUnitConfig(ArticleDraftRevisionTest.Config.class)
class ArticleDraftRevisionTest {
    private static final String CONTENT = "{\"type\":\"markdown\",\"version\":1,\"source\":\"原始正文\"}";
    private static final CallerContext AUTHOR = caller("u-author");

    @Autowired private ArticleTransactionService transactions;
    @Autowired private ArticleRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void autosaveOnlyUpdatesCurrentContentAndManualSaveCreatesDistinctSnapshots() {
        StoredArticle first = draft(true);
        String id = first.article().id();
        assertThat(first.revision()).isEqualTo(1);
        assertThat(repository.findContentVersions(id)).isEmpty();

        StoredArticle updated = update(id, "第二版", 1, true);
        assertThat(updated.revision()).isEqualTo(2);
        assertThat(updated.article().updatedAt()).isAfterOrEqualTo(first.article().updatedAt());
        assertThat(repository.findContentVersions(id)).isEmpty();

        StoredArticle manual = update(id, "第二版", 2, false);
        assertThat(manual.revision()).isEqualTo(2);
        assertThat(repository.findContentVersions(id)).hasSize(1);
        update(id, "第二版", 2, false);
        assertThat(repository.findContentVersions(id)).hasSize(1);
        update(id, "第三版", 2, true);
        transactions.publish(id, ArticleVisibilityType.COMPANY, Set.of(), 3, AUTHOR.userId());
        assertThat(repository.findContentVersions(id)).extracting(ArticleContentVersion::title)
                .containsExactly("第二版", "第三版");
        assertThat(repository.findById(id).orElseThrow().revision()).isEqualTo(4);
    }

    @Test
    void retriesLostSaveResponseWithoutOverwritingLaterContent() {
        String id = draft(false).article().id();
        StoredArticle saved = update(id, "保存后", 1, false);
        StoredArticle retry = update(id, "保存后", 1, false);
        assertThat(retry.revision()).isEqualTo(saved.revision());
        assertThat(retry.article().updatedAt()).isCloseTo(saved.article().updatedAt(), within(1, ChronoUnit.MICROS));
        assertThat(repository.findContentVersions(id)).hasSize(2);
        assertConflict(() -> update(id, "另一个修改", 1, false));
        assertThat(repository.findById(id).orElseThrow().article().title()).isEqualTo("保存后");
        assertThat(repository.findContentVersions(id)).hasSize(2);
        assertThat(outboxCount(id)).isZero();
    }

    @Test
    void manualRetryAfterLostAutosaveResponseStillCreatesOneSnapshot() {
        String id = draft(true).article().id();
        update(id, "已自动保存", 1, true);
        assertThat(update(id, "已自动保存", 1, false).revision()).isEqualTo(2);
        update(id, "已自动保存", 1, false);
        assertThat(repository.findContentVersions(id)).singleElement()
                .satisfies(version -> assertThat(version.title()).isEqualTo("已自动保存"));

        ArticleService service = service((caller, article, request) -> { }, new AtomicInteger());
        String clientId = UUID.randomUUID().toString();
        String createdId = service.saveDraft(AUTHOR, create("创建", clientId, true)).id();
        service.saveDraft(AUTHOR, create("创建", clientId, false));
        service.saveDraft(AUTHOR, create("创建", clientId, false));
        assertThat(repository.findContentVersions(createdId)).hasSize(1);
        update(createdId, "后续自动保存", 1, true);
        service.saveDraft(AUTHOR, create("创建", clientId, false));
        assertThat(repository.findContentVersions(createdId)).hasSize(1);
    }

    @Test
    void revisionAndSnapshotsIncludeBodyTagsAndCategory() {
        String id = draft(false).article().id();
        String changedContent = "{\"type\":\"markdown\",\"version\":1,\"source\":\"## 修改后的源文\\n\\n\"}";
        StoredArticle changed = transactions.updateDraft(id, "原稿", changedContent, Set.of("java", "redis"), "engineering",
                AUTHOR.userId(), 1, true);
        assertThat(changed.revision()).isEqualTo(2);
        assertThat(repository.findContentVersions(id)).hasSize(1);
        assertConflict(() -> transactions.updateDraft(id, "原稿", changedContent, Set.of("java"), "engineering",
                AUTHOR.userId(), 1, true));
        assertConflict(() -> transactions.updateDraft(id, "原稿", changedContent, Set.of("java", "redis"), null,
                AUTHOR.userId(), 1, true));
        assertConflict(() -> update(id, "原稿", 1, true));
        transactions.updateDraft(id, "原稿", changedContent, Set.of("redis", "java"), "engineering",
                AUTHOR.userId(), 2, false);
        assertThat(repository.findContentVersions(id)).hasSize(2);
        assertThat(repository.findLatestContentVersion(id).orElseThrow()).satisfies(version -> {
            assertThat(version.contentJson()).isEqualTo(changedContent);
            assertThat(version.tagIds()).containsExactlyInAnyOrder("java", "redis");
            assertThat(version.categoryId()).isEqualTo("engineering");
        });
    }

    @Test
    void concurrentEditsHaveOneWinnerAndOneConflict() throws Exception {
        String id = draft(false).article().id();
        List<String> results = race(
                () -> update(id, "标签页 A", 1, false).article().title(),
                () -> update(id, "标签页 B", 1, false).article().title());
        assertThat(results).contains("409");
        assertThat(results.stream().filter("409"::equals).count()).isEqualTo(1);
        StoredArticle current = repository.findById(id).orElseThrow();
        assertThat(current.revision()).isEqualTo(2);
        assertThat(results).contains(current.article().title());
        assertThat(repository.findContentVersions(id)).hasSize(2);
        assertThat(repository.findLatestContentVersion(id).orElseThrow().title()).isEqualTo(current.article().title());
    }

    @Test
    void concurrentCreateWithSameClientIdCommitsOnlyOneCompleteAggregate() throws Exception {
        ArticleService service = service((caller, article, request) -> { }, new AtomicInteger());
        String clientId = UUID.randomUUID().toString();
        List<String> ids = race(
                () -> service.saveDraft(AUTHOR, create("first", clientId, false)).id(),
                () -> service.saveDraft(AUTHOR, create("second", clientId, false)).id());
        assertThat(ids.get(0)).isEqualTo(ids.get(1));
        StoredArticle saved = repository.findById(ids.get(0)).orElseThrow();
        assertThat(saved.revision()).isEqualTo(1);
        assertThat(saved.article().title()).isIn("first", "second");
        assertThat(repository.findContentVersions(ids.get(0))).singleElement()
                .satisfies(version -> assertThat(version.title()).isEqualTo(saved.article().title()));
    }

    @Test
    void clientDraftIdIsAuthorScopedAndCreateRetryNeverOverwritesExistingDraft() {
        ArticleService service = service((caller, article, request) -> { }, new AtomicInteger());
        String clientId = UUID.randomUUID().toString();
        ArticleResponse first = service.saveDraft(AUTHOR, create("原稿", clientId, true));
        update(first.id(), "更新稿", first.revision(), true);
        ArticleResponse retried = service.saveDraft(AUTHOR, create("丢失的创建响应", clientId, true));
        ArticleResponse other = service.saveDraft(caller("other"), create("他人草稿", clientId, true));
        assertThat(retried.id()).isEqualTo(first.id());
        assertThat(retried.title()).isEqualTo("更新稿");
        assertThat(retried.revision()).isEqualTo(2);
        assertThat(other.id()).isNotEqualTo(first.id());
        assertThat(other.authorId()).isEqualTo("other");
        assertThatThrownBy(() -> service.saveDraft(AUTHOR, create("bad", "not-a-uuid", true)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void conflictsDoNotCreateVersionsOutboxOrReviewTickets() {
        AtomicInteger tickets = new AtomicInteger();
        ArticleService service = service((caller, article, request) -> { }, tickets);
        String id = draft(true).article().id();
        update(id, "新正文", 1, true);
        assertConflict(() -> service.submitForPublish(id, AUTHOR,
                new SubmitPublishRequest("TEAM", Set.of("t-search"), true, 1L)));
        assertConflict(() -> service.submitForPublish(id, AUTHOR,
                new SubmitPublishRequest("COMPANY", Set.of(), false, 1L)));
        StoredArticle current = repository.findById(id).orElseThrow();
        assertThat(current.article().status()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(current.article().visibilityType()).isNull();
        assertThat(current.article().reviewRequestId()).isNull();
        assertThat(current.revision()).isEqualTo(2);
        assertThat(tickets).hasValue(0);
        assertThat(outboxCount(id)).isZero();
        assertThat(repository.findContentVersions(id)).isEmpty();
    }

    @Test
    void autosaveAndPublishRaceCannotPublishAnUnacknowledgedRevision() throws Exception {
        String id = draft(true).article().id();
        List<String> results = race(
                () -> { update(id, "正在输入", 1, true); return "save"; },
                () -> { transactions.publish(id, ArticleVisibilityType.COMPANY, Set.of(), 1, AUTHOR.userId()); return "publish"; });
        assertThat(results.stream().filter("409"::equals).count()).isEqualTo(1);
        StoredArticle current = repository.findById(id).orElseThrow();
        assertThat(current.revision()).isEqualTo(2);
        if (results.contains("publish")) {
            assertThat(current.article().status()).isEqualTo(ArticleStatus.PUBLISHED);
            assertThat(current.article().title()).isEqualTo("原稿");
            assertThat(outboxCount(id)).isGreaterThan(0);
            assertThat(repository.findContentVersions(id)).hasSize(1);
        } else {
            assertThat(current.article().status()).isEqualTo(ArticleStatus.DRAFT);
            assertThat(current.article().title()).isEqualTo("正在输入");
            assertThat(outboxCount(id)).isZero();
            assertThat(repository.findContentVersions(id)).isEmpty();
        }
    }

    @Test
    void reviewRejectionAndWithdrawalAdvanceRevisionAndRejectStalePublish() {
        String id = draft(true).article().id();
        StoredArticle pending = transactions.requestReview(id, ArticleVisibilityType.TEAM, Set.of("t-search"), 1, AUTHOR.userId());
        assertThat(pending.revision()).isEqualTo(2);
        String requestId = pending.article().reviewRequestId();
        assertThat(transactions.rejectFromReview(id, "ticket", requestId).revision()).isEqualTo(3);
        assertThat(transactions.rejectFromReview(id, "ticket", requestId).revision()).isEqualTo(3);
        assertConflict(() -> transactions.publish(id, ArticleVisibilityType.COMPANY, Set.of(), 1, AUTHOR.userId()));
        transactions.publish(id, ArticleVisibilityType.COMPANY, Set.of(), 3, AUTHOR.userId());
        assertThat(transactions.withdraw(id).revision()).isEqualTo(5);
        assertConflict(() -> update(id, "过期编辑", 1, true));
        assertThat(update(id, "原稿", 5, true).revision()).isEqualTo(6);
        assertConflict(() -> transactions.publish(id, ArticleVisibilityType.COMPANY, Set.of(), 3, AUTHOR.userId()));
        assertThat(transactions.delete(id).revision()).isEqualTo(7);
        assertThat(transactions.delete(id).revision()).isEqualTo(7);
    }

    @Test
    void publicationRetryIsIdempotentAndApprovalOnlyAdvancesRevisionOnce() {
        String id = draft(true).article().id();
        transactions.publish(id, ArticleVisibilityType.COMPANY, Set.of(), 1, AUTHOR.userId());
        int events = outboxCount(id);
        assertThat(transactions.publish(id, ArticleVisibilityType.COMPANY, Set.of(), 1, AUTHOR.userId()).revision()).isEqualTo(2);
        assertThat(outboxCount(id)).isEqualTo(events);
        assertThat(repository.findContentVersions(id)).hasSize(1);

        String reviewed = draft(true).article().id();
        StoredArticle pending = transactions.requestReview(reviewed, ArticleVisibilityType.TEAM, Set.of("t-search"), 1, AUTHOR.userId());
        String requestId = pending.article().reviewRequestId();
        assertThat(transactions.approveFromReview(reviewed, "ticket", requestId).revision()).isEqualTo(3);
        events = outboxCount(reviewed);
        assertThat(transactions.approveFromReview(reviewed, "ticket", requestId).revision()).isEqualTo(3);
        assertThat(outboxCount(reviewed)).isEqualTo(events);
    }

    @Test
    void retriesCannotBypassAuthorization() {
        AtomicBoolean deny = new AtomicBoolean();
        PermissionCheckClient permission = new PermissionCheckClient() {
            @Override public void requirePublishAllowed(CallerContext caller, Article article, SubmitPublishRequest request) {
                requireEditAllowed(caller, article);
            }
            @Override public void requireEditAllowed(CallerContext caller, Article article) {
                if (deny.get()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        };
        AtomicInteger tickets = new AtomicInteger();
        ArticleService service = service(permission, tickets);
        String clientId = UUID.randomUUID().toString();
        String id = service.saveDraft(AUTHOR, create("原稿", clientId, true)).id();
        service.updateDraft(id, AUTHOR, new UpdateDraftRequest("新稿", CONTENT, Set.of(), null, 1L, true));
        deny.set(true);
        assertForbidden(() -> service.updateDraft(id, AUTHOR, new UpdateDraftRequest("新稿", CONTENT, Set.of(), null, 1L, true)));
        assertForbidden(() -> service.saveDraft(new CallerContext(AUTHOR.userId(), Set.of("READER"), Set.of(), Set.of()), create("原稿", clientId, true)));
        deny.set(false);
        SubmitPublishRequest request = new SubmitPublishRequest("TEAM", Set.of("t-search"), true, 2L);
        service.submitForPublish(id, AUTHOR, request);
        deny.set(true);
        assertForbidden(() -> service.submitForPublish(id, AUTHOR, request));
        assertThat(tickets).hasValue(1);
    }

    private ArticleService service(PermissionCheckClient permission, AtomicInteger tickets) {
        return new ArticleService(repository, tags -> { }, permission, transactions,
                SubmitPublishRequest::reviewRequired, (article, request) -> tickets.incrementAndGet(), (type, targets) -> { });
    }

    private StoredArticle draft(boolean autosave) {
        return transactions.saveDraft(new StoredArticle(Article.draft(UUID.randomUUID().toString(), AUTHOR.userId(), "原稿"),
                CONTENT, ArticleContentProjection.from(CONTENT), Set.of()), AUTHOR.userId(), autosave);
    }

    private StoredArticle update(String id, String title, long revision, boolean autosave) {
        return transactions.updateDraft(id, title, CONTENT, Set.of(), null, AUTHOR.userId(), revision, autosave);
    }

    private static SaveDraftRequest create(String title, String clientId, boolean autosave) {
        return new SaveDraftRequest(title, CONTENT, Set.of(), null, clientId, autosave);
    }

    private static CallerContext caller(String user) {
        return new CallerContext(user, Set.of("AUTHOR"), Set.of(), Set.of("t-search"));
    }

    private int outboxCount(String id) {
        return jdbc.queryForObject("select count(*) from domain_event where aggregate_id = ?", Integer.class, id)
                + jdbc.queryForObject("select count(*) from article_subscription_notification_event where article_id = ?", Integer.class, id);
    }

    private static void assertConflict(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    private static void assertForbidden(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static List<String> race(Callable<String> first, Callable<String> second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var a = executor.submit(() -> competingCall(first, ready, start));
            var b = executor.submit(() -> competingCall(second, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private static String competingCall(Callable<String> action, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("concurrent test timed out");
        try {
            return action.call();
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) return "409";
            throw ex;
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() {
            var source = new DriverManagerDataSource("jdbc:h2:mem:article_draft_revision;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
            Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
            return source;
        }
        @Bean JdbcTemplate jdbcTemplate(DataSource source) { return new JdbcTemplate(source); }
        @Bean ArticleRepository articleRepository(JdbcTemplate jdbc) { return new JdbcArticleRepository(jdbc); }
        @Bean ArticleOutbox articleOutbox(JdbcTemplate jdbc) { return new JdbcArticleOutbox(jdbc); }
        @Bean ArticleTransactionService transactionService(ArticleRepository repository, ArticleOutbox outbox) {
            return new ArticleTransactionService(repository, outbox);
        }
        @Bean PlatformTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
    }
}
