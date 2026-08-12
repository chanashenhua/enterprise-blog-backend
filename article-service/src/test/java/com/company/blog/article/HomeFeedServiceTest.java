package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.article.api.ArticleEngagement;
import com.company.blog.article.api.ArticleMemoryRepository;
import com.company.blog.article.api.CallerContext;
import com.company.blog.article.api.FeedSubscription;
import com.company.blog.article.api.HomeFeedResponse;
import com.company.blog.article.api.HomeFeedService;
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

class HomeFeedServiceTest {
    @Test
    void buildsLatestPopularAndSubscribedSectionsFromVisiblePublishedArticles() {
        ArticleMemoryRepository repository = new ArticleMemoryRepository();
        repository.save(article("popular", "热门实践", "数据库经验", Set.of("postgresql"), "database", 1));
        repository.save(article("subscribed", "Java 新实践", "团队最新经验", Set.of("java"), "engineering", 3));
        repository.save(article("restricted", "不可见内容", "不应出现在首页", Set.of("java"), "engineering", 4));

        PermissionCheckClient permissions = new PermissionCheckClient() {
            @Override
            public void requirePublishAllowed(CallerContext caller, Article article, SubmitPublishRequest request) {
            }

            @Override
            public boolean readAllowed(CallerContext caller, Article article) {
                return !article.id().equals("restricted");
            }
        };
        HomeFeedService service = new HomeFeedService(
                repository,
                permissions,
                limit -> List.of(
                        new ArticleEngagement("popular", 80, 12, 5),
                        new ArticleEngagement("restricted", 100, 20, 10),
                        new ArticleEngagement("subscribed", 10, 2, 1)
                ),
                userId -> List.of(new FeedSubscription("TAG", "java"))
        );

        HomeFeedResponse response = service.home(caller(), 2);

        assertThat(response.latest()).extracting(item -> item.articleId())
                .containsExactly("subscribed", "popular");
        assertThat(response.popular()).extracting(item -> item.articleId())
                .containsExactly("popular", "subscribed");
        assertThat(response.subscribed()).extracting(item -> item.articleId())
                .containsExactly("subscribed");
        assertThat(response.popular().get(0).viewCount()).isEqualTo(80);
        assertThat(response.latest().get(0).summary()).isEqualTo("团队最新经验");
        assertThat(response.generatedAt()).isNotNull();
    }

    @Test
    void fallsBackToLatestArticlesWhenThereIsNoEngagementRanking() {
        ArticleMemoryRepository repository = new ArticleMemoryRepository();
        repository.save(article("latest", "最新文章", "正文", Set.of(), null, 2));
        HomeFeedService service = new HomeFeedService(
                repository,
                permissionsAllowingAll(),
                limit -> List.of(),
                userId -> List.of()
        );

        HomeFeedResponse response = service.home(caller(), 6);

        assertThat(response.popular()).extracting(item -> item.articleId()).containsExactly("latest");
        assertThat(response.subscribed()).isEmpty();
    }

    @Test
    void keepsLatestFeedAvailableWhenOptionalServicesAreDown() {
        ArticleMemoryRepository repository = new ArticleMemoryRepository();
        repository.save(article("latest", "可用文章", "正文", Set.of("java"), "engineering", 2));
        HomeFeedService service = new HomeFeedService(
                repository,
                permissionsAllowingAll(),
                limit -> { throw new IllegalStateException("stats unavailable"); },
                userId -> { throw new IllegalStateException("subscriptions unavailable"); }
        );

        HomeFeedResponse response = service.home(caller(), 6);

        assertThat(response.latest()).extracting(item -> item.articleId()).containsExactly("latest");
        assertThat(response.popular()).extracting(item -> item.articleId()).containsExactly("latest");
        assertThat(response.subscribed()).isEmpty();
    }

    private static PermissionCheckClient permissionsAllowingAll() {
        return (caller, article, request) -> {
        };
    }

    private static CallerContext caller() {
        return new CallerContext("u-reader", Set.of("READER"), Set.of("d-platform"), Set.of("t-search"));
    }

    private static StoredArticle article(
            String id,
            String title,
            String plainText,
            Set<String> tagIds,
            String categoryId,
            long publishedMinute
    ) {
        Instant createdAt = Instant.parse("2026-08-11T08:00:00Z");
        Article article = Article.rehydrate(
                id,
                "u-author",
                title,
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
                new ArticleContentProjection("<p>" + plainText + "</p>", plainText),
                tagIds,
                categoryId
        );
    }
}
