package com.company.blog.article.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArticleDiscoveryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleDiscoveryService.class);
    private static final Pattern TARGET_ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}");
    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_CANDIDATES = 100;

    private final ArticleRepository repository;
    private final PermissionCheckClient permissionCheckClient;
    private final ArticleEngagementClient engagementClient;

    public ArticleDiscoveryService(
            ArticleRepository repository,
            PermissionCheckClient permissionCheckClient,
            ArticleEngagementClient engagementClient
    ) {
        this.repository = repository;
        this.permissionCheckClient = permissionCheckClient;
        this.engagementClient = engagementClient;
    }

    public ArticleDiscoveryResponse discover(
            CallerContext caller,
            String requestedType,
            String requestedTargetId,
            int requestedLimit
    ) {
        requireCaller(caller);
        DiscoveryTargetType targetType = DiscoveryTargetType.from(requestedType);
        String targetId = requireTargetId(requestedTargetId);
        int limit = normalizeLimit(requestedLimit);
        int candidateLimit = Math.min(MAX_CANDIDATES, Math.max(limit * 3, DEFAULT_LIMIT));
        List<StoredArticle> candidates = switch (targetType) {
            case CATEGORY -> repository.findPublishedByCategory(targetId, candidateLimit);
            case TAG -> repository.findPublishedByTag(targetId, candidateLimit);
        };
        Map<String, ArticleEngagement> engagementByArticle = engagementByArticle();
        List<HomeFeedItem> items = candidates.stream()
                .filter(article -> permissionCheckClient.readAllowed(caller, article.article()))
                .limit(limit)
                .map(article -> HomeFeedItemFactory.from(
                        article,
                        engagementByArticle.get(article.article().id())
                ))
                .toList();
        return new ArticleDiscoveryResponse(targetType, targetId, items, Instant.now());
    }

    private Map<String, ArticleEngagement> engagementByArticle() {
        Map<String, ArticleEngagement> result = new LinkedHashMap<>();
        try {
            engagementClient.topArticles(MAX_CANDIDATES)
                    .forEach(item -> result.putIfAbsent(item.articleId(), item));
        } catch (RuntimeException exception) {
            LOGGER.warn("Interaction rankings are unavailable for article discovery", exception);
        }
        return result;
    }

    private static int normalizeLimit(int requestedLimit) {
        if (requestedLimit <= 0) return DEFAULT_LIMIT;
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private static String requireTargetId(String targetId) {
        if (targetId == null || !TARGET_ID.matcher(targetId.trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid discovery target id is required");
        }
        return targetId.trim();
    }

    private static void requireCaller(CallerContext caller) {
        if (caller == null || caller.userId() == null || caller.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
    }
}
