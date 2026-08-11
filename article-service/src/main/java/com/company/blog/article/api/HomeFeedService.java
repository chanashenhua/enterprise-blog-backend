package com.company.blog.article.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HomeFeedService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeFeedService.class);
    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_SECTION_SIZE = 20;
    private static final int MAX_CANDIDATES = 100;
    private static final int SUMMARY_LENGTH = 150;

    private final ArticleRepository repository;
    private final PermissionCheckClient permissionCheckClient;
    private final ArticleEngagementClient engagementClient;
    private final FeedSubscriptionClient subscriptionClient;

    public HomeFeedService(
            ArticleRepository repository,
            PermissionCheckClient permissionCheckClient,
            ArticleEngagementClient engagementClient,
            FeedSubscriptionClient subscriptionClient
    ) {
        this.repository = repository;
        this.permissionCheckClient = permissionCheckClient;
        this.engagementClient = engagementClient;
        this.subscriptionClient = subscriptionClient;
    }

    public HomeFeedResponse home(CallerContext caller, int requestedLimit) {
        requireCaller(caller);
        int limit = normalizeLimit(requestedLimit);
        int candidateLimit = Math.min(MAX_CANDIDATES, Math.max(limit * 5, 30));

        List<StoredArticle> visibleArticles = repository.findPublished(candidateLimit).stream()
                .filter(article -> permissionCheckClient.readAllowed(caller, article.article()))
                .toList();
        Map<String, StoredArticle> visibleById = new LinkedHashMap<>();
        visibleArticles.forEach(article -> visibleById.put(article.article().id(), article));

        List<ArticleEngagement> rankings = rankings();
        Map<String, ArticleEngagement> engagementByArticle = new LinkedHashMap<>();
        rankings.forEach(item -> engagementByArticle.putIfAbsent(item.articleId(), item));

        List<HomeFeedItem> latest = visibleArticles.stream()
                .limit(limit)
                .map(article -> item(article, engagementByArticle.get(article.article().id())))
                .toList();
        List<HomeFeedItem> popular = popular(
                rankings,
                visibleById,
                engagementByArticle,
                latest,
                limit
        );
        List<FeedSubscription> subscriptions = subscriptions(caller.userId());
        List<HomeFeedItem> subscribed = visibleArticles.stream()
                .filter(article -> matchesSubscription(article, subscriptions))
                .limit(limit)
                .map(article -> item(article, engagementByArticle.get(article.article().id())))
                .toList();

        return new HomeFeedResponse(latest, popular, subscribed, Instant.now());
    }

    public HomeFeedResponse home(CallerContext caller) {
        return home(caller, DEFAULT_LIMIT);
    }

    private List<ArticleEngagement> rankings() {
        try {
            return engagementClient.topArticles(MAX_CANDIDATES);
        } catch (RuntimeException exception) {
            LOGGER.warn("Interaction rankings are unavailable; falling back to the latest feed", exception);
            return List.of();
        }
    }

    private List<FeedSubscription> subscriptions(String userId) {
        try {
            return subscriptionClient.findByUser(userId);
        } catch (RuntimeException exception) {
            LOGGER.warn("Subscriptions are unavailable for user {}; returning an empty section", userId, exception);
            return List.of();
        }
    }

    private static List<HomeFeedItem> popular(
            List<ArticleEngagement> rankings,
            Map<String, StoredArticle> visibleById,
            Map<String, ArticleEngagement> engagementByArticle,
            List<HomeFeedItem> latest,
            int limit
    ) {
        List<HomeFeedItem> result = new ArrayList<>();
        Set<String> included = new LinkedHashSet<>();
        for (ArticleEngagement ranking : rankings) {
            StoredArticle article = visibleById.get(ranking.articleId());
            if (article == null || !included.add(ranking.articleId())) continue;
            result.add(item(article, ranking));
            if (result.size() == limit) return List.copyOf(result);
        }
        for (HomeFeedItem item : latest) {
            if (included.add(item.articleId())) result.add(item);
            if (result.size() == limit) break;
        }
        return List.copyOf(result);
    }

    private static boolean matchesSubscription(
            StoredArticle article,
            List<FeedSubscription> subscriptions
    ) {
        for (FeedSubscription subscription : subscriptions) {
            if (subscription == null || subscription.targetType() == null || subscription.targetId() == null) continue;
            String type = subscription.targetType().trim().toUpperCase(Locale.ROOT);
            String targetId = subscription.targetId().trim();
            if (type.equals("CATEGORY") && targetId.equals(article.categoryId())) return true;
            if (type.equals("TAG") && article.tagIds().contains(targetId)) return true;
        }
        return false;
    }

    private static HomeFeedItem item(StoredArticle storedArticle, ArticleEngagement engagement) {
        ArticleEngagement metrics = engagement == null
                ? ArticleEngagement.empty(storedArticle.article().id())
                : engagement;
        return new HomeFeedItem(
                storedArticle.article().id(),
                storedArticle.article().authorId(),
                storedArticle.article().title(),
                summary(storedArticle.content().plainText()),
                storedArticle.tagIds(),
                storedArticle.categoryId(),
                storedArticle.article().updatedAt(),
                metrics.viewCount(),
                metrics.likeCount(),
                metrics.favoriteCount()
        );
    }

    private static String summary(String plainText) {
        if (plainText == null || plainText.isBlank()) return "暂无摘要";
        String normalized = plainText.trim().replaceAll("\\s+", " ");
        return normalized.length() <= SUMMARY_LENGTH
                ? normalized
                : normalized.substring(0, SUMMARY_LENGTH).stripTrailing() + "…";
    }

    private static int normalizeLimit(int requestedLimit) {
        if (requestedLimit <= 0) return DEFAULT_LIMIT;
        return Math.min(requestedLimit, MAX_SECTION_SIZE);
    }

    private static void requireCaller(CallerContext caller) {
        if (caller == null || caller.userId() == null || caller.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
    }
}
