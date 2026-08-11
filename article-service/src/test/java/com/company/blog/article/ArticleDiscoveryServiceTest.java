package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.article.api.ArticleDiscoveryResponse;
import com.company.blog.article.api.ArticleDiscoveryService;
import com.company.blog.article.api.ArticleEngagement;
import com.company.blog.article.api.ArticleMemoryRepository;
import com.company.blog.article.api.CallerContext;
import com.company.blog.article.api.DiscoveryTargetType;
import com.company.blog.article.api.PermissionCheckClient;
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

class ArticleDiscoveryServiceTest {
    @Test
    void discoversVisibleArticlesByCategoryAndTag() {
        ArticleMemoryRepository repository = new ArticleMemoryRepository();
        repository.save(article("java-guide", Set.of("java"), "engineering", 3));
        repository.save(article("database-guide", Set.of("postgresql"), "database", 2));
        repository.save(article("restricted", Set.of("java"), "engineering", 4));
        PermissionCheckClient permissions = new PermissionCheckClient() {
            @Override
            public void requirePublishAllowed(CallerContext caller, Article article, SubmitPublishRequest request) {
            }

            @Override
            public boolean readAllowed(CallerContext caller, Article article) {
                return !article.id().equals("restricted");
            }
        };
        ArticleDiscoveryService service = new ArticleDiscoveryService(
                repository,
                permissions,
                limit -> List.of(new ArticleEngagement("java-guide", 18, 4, 2))
        );

        ArticleDiscoveryResponse category = service.discover(caller(), "category", "engineering", 20);
        ArticleDiscoveryResponse tag = service.discover(caller(), "TAG", "java", 20);

        assertThat(category.targetType()).isEqualTo(DiscoveryTargetType.CATEGORY);
        assertThat(category.items()).extracting(item -> item.articleId()).containsExactly("java-guide");
        assertThat(tag.targetType()).isEqualTo(DiscoveryTargetType.TAG);
        assertThat(tag.items()).extracting(item -> item.articleId()).containsExactly("java-guide");
        assertThat(tag.items().get(0).viewCount()).isEqualTo(18);
    }

    @Test
    void rejectsUnknownTypesAndInvalidTargetIds() {
        ArticleDiscoveryService service = new ArticleDiscoveryService(
                new ArticleMemoryRepository(),
                (caller, article, request) -> { },
                limit -> List.of()
        );

        assertThatThrownBy(() -> service.discover(caller(), "AUTHOR", "u-author", 20))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.discover(caller(), "TAG", "Java Script", 20))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private static CallerContext caller() {
        return new CallerContext("u-reader", Set.of("READER"), Set.of("d-platform"), Set.of("t-search"));
    }

    private static StoredArticle article(
            String id,
            Set<String> tagIds,
            String categoryId,
            long publishedMinute
    ) {
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
                createdAt.plusSeconds(publishedMinute * 60)
        );
        return new StoredArticle(
                article,
                "{}",
                new ArticleContentProjection("<p>发现正文</p>", "发现正文"),
                tagIds,
                categoryId
        );
    }
}
