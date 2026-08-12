package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.article.api.ArticleEngagement;
import com.company.blog.article.api.ArticleMemoryRepository;
import com.company.blog.article.api.CallerContext;
import com.company.blog.article.api.KnowledgeCollection;
import com.company.blog.article.api.KnowledgeCollectionDetailResponse;
import com.company.blog.article.api.KnowledgeCollectionMemoryRepository;
import com.company.blog.article.api.KnowledgeCollectionService;
import com.company.blog.article.api.PermissionCheckClient;
import com.company.blog.article.api.SaveKnowledgeCollectionRequest;
import com.company.blog.article.api.StoredArticle;
import com.company.blog.article.api.SubmitPublishRequest;
import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import com.company.blog.article.domain.ArticleStatus;
import com.company.blog.article.domain.ArticleVisibilityType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class KnowledgeCollectionServiceTest {
    @Test
    void createsAndReordersACollectionOfVisiblePublishedArticles() {
        ArticleMemoryRepository articles = new ArticleMemoryRepository();
        articles.save(published("a-one", 1));
        articles.save(published("a-two", 2));
        KnowledgeCollectionMemoryRepository collections = new KnowledgeCollectionMemoryRepository();
        KnowledgeCollectionService service = service(collections, articles, Set.of());

        KnowledgeCollectionDetailResponse created = service.create(
                caller("u-author", "AUTHOR"),
                new SaveKnowledgeCollectionRequest("Java 入门路径", "从语言基础到服务实践", List.of("a-one", "a-two"))
        );
        KnowledgeCollectionDetailResponse updated = service.update(
                created.id(),
                caller("u-author", "AUTHOR"),
                new SaveKnowledgeCollectionRequest("Java 成长路径", "按学习顺序阅读", List.of("a-two", "a-one"))
        );

        assertThat(created.editable()).isTrue();
        assertThat(updated.articles()).extracting(item -> item.articleId())
                .containsExactly("a-two", "a-one");
        assertThat(service.list(caller("u-reader", "READER"), false, 20))
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.title()).isEqualTo("Java 成长路径");
                    assertThat(summary.articleCount()).isEqualTo(2);
                    assertThat(summary.editable()).isFalse();
                });
    }

    @Test
    void filtersUnreadableArticlesAndRestrictsMaintenanceToOwnerOrAdmin() {
        ArticleMemoryRepository articles = new ArticleMemoryRepository();
        articles.save(published("a-visible", 1));
        articles.save(published("a-hidden", 2));
        KnowledgeCollectionMemoryRepository collections = new KnowledgeCollectionMemoryRepository();
        Instant now = Instant.parse("2026-08-11T10:00:00Z");
        collections.save(new KnowledgeCollection(
                "kc-security",
                "u-author",
                "受限专题",
                "读取时重新校验权限",
                List.of("a-visible", "a-hidden"),
                now,
                now
        ));
        KnowledgeCollectionService service = service(collections, articles, Set.of("a-hidden"));

        assertThat(service.get("kc-security", caller("u-reader", "READER")).articles())
                .extracting(item -> item.articleId())
                .containsExactly("a-visible");
        assertThatThrownBy(() -> service.update(
                "kc-security",
                caller("u-reader", "READER"),
                new SaveKnowledgeCollectionRequest("不能修改", "", List.of("a-visible", "a-hidden"))
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        KnowledgeCollectionDetailResponse adminUpdate = service.update(
                "kc-security",
                caller("u-admin", "ADMIN"),
                new SaveKnowledgeCollectionRequest("管理员整理", "", List.of("a-hidden", "a-visible"))
        );
        assertThat(adminUpdate.editable()).isTrue();
    }

    @Test
    void rejectsDraftsDuplicatesAndCollectionsWithFewerThanTwoArticles() {
        ArticleMemoryRepository articles = new ArticleMemoryRepository();
        articles.save(published("a-visible", 1));
        articles.save(draft("a-draft"));
        KnowledgeCollectionService service = service(
                new KnowledgeCollectionMemoryRepository(),
                articles,
                Set.of()
        );
        CallerContext caller = caller("u-author", "AUTHOR");

        assertThatThrownBy(() -> service.create(
                caller,
                new SaveKnowledgeCollectionRequest("文章不足", "", List.of("a-visible"))
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.create(
                caller,
                new SaveKnowledgeCollectionRequest("文章重复", "", List.of("a-visible", "a-visible"))
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.create(
                caller,
                new SaveKnowledgeCollectionRequest("包含草稿", "", List.of("a-visible", "a-draft"))
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private static KnowledgeCollectionService service(
            KnowledgeCollectionMemoryRepository collections,
            ArticleMemoryRepository articles,
            Set<String> hiddenArticleIds
    ) {
        PermissionCheckClient permissions = new PermissionCheckClient() {
            @Override
            public void requirePublishAllowed(CallerContext caller, Article article, SubmitPublishRequest request) {
            }

            @Override
            public boolean readAllowed(CallerContext caller, Article article) {
                return !hiddenArticleIds.contains(article.id()) || caller.roles().contains("ADMIN");
            }
        };
        return new KnowledgeCollectionService(
                collections,
                articles,
                permissions,
                limit -> List.of(new ArticleEngagement("a-one", 12, 3, 2))
        );
    }

    private static CallerContext caller(String userId, String role) {
        return new CallerContext(userId, Set.of(role), Set.of(), Set.of());
    }

    private static StoredArticle published(String id, int minute) {
        Instant createdAt = Instant.parse("2026-08-11T08:00:00Z");
        Article article = Article.rehydrate(
                id,
                "u-author",
                id + " 标题",
                ArticleStatus.PUBLISHED,
                ArticleVisibilityType.COMPANY,
                Set.of(),
                null,
                null,
                null,
                createdAt,
                createdAt.plusSeconds(minute * 60L)
        );
        return stored(article);
    }

    private static StoredArticle draft(String id) {
        return stored(Article.draft(id, "u-author", id + " 草稿"));
    }

    private static StoredArticle stored(Article article) {
        return new StoredArticle(
                article,
                "{}",
                new ArticleContentProjection("<p>专题正文</p>", "专题正文"),
                Set.of("java"),
                "engineering"
        );
    }
}
