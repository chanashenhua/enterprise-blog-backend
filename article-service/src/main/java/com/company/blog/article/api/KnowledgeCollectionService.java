package com.company.blog.article.api;

import com.company.blog.article.domain.ArticleStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KnowledgeCollectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeCollectionService.class);
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_ARTICLES = 30;
    private static final int MIN_ARTICLES = 2;
    private static final int MAX_CANDIDATES = 100;

    private final KnowledgeCollectionRepository collectionRepository;
    private final ArticleRepository articleRepository;
    private final PermissionCheckClient permissionCheckClient;
    private final ArticleEngagementClient engagementClient;

    public KnowledgeCollectionService(
            KnowledgeCollectionRepository collectionRepository,
            ArticleRepository articleRepository,
            PermissionCheckClient permissionCheckClient,
            ArticleEngagementClient engagementClient
    ) {
        this.collectionRepository = collectionRepository;
        this.articleRepository = articleRepository;
        this.permissionCheckClient = permissionCheckClient;
        this.engagementClient = engagementClient;
    }

    public List<KnowledgeCollectionSummary> list(CallerContext caller, boolean mine, int requestedLimit) {
        requireCaller(caller);
        int limit = normalizeLimit(requestedLimit);
        List<KnowledgeCollection> collections = mine
                ? collectionRepository.findByOwnerId(caller.userId(), limit)
                : collectionRepository.findRecent(MAX_LIMIT);
        return collections.stream()
                .map(collection -> summary(collection, caller))
                .filter(summary -> mine || summary.articleCount() > 0 || summary.editable())
                .limit(limit)
                .toList();
    }

    public KnowledgeCollectionDetailResponse get(String collectionId, CallerContext caller) {
        requireCaller(caller);
        return detail(findCollection(collectionId), caller);
    }

    public List<HomeFeedItem> candidateArticles(CallerContext caller, int requestedLimit) {
        requireCaller(caller);
        int limit = normalizeLimit(requestedLimit);
        Map<String, ArticleEngagement> engagement = engagementByArticle();
        return articleRepository.findPublished(MAX_CANDIDATES).stream()
                .filter(article -> permissionCheckClient.readAllowed(caller, article.article()))
                .limit(limit)
                .map(article -> HomeFeedItemFactory.from(
                        article,
                        engagement.get(article.article().id())
                ))
                .toList();
    }

    public KnowledgeCollectionDetailResponse create(
            CallerContext caller,
            SaveKnowledgeCollectionRequest request
    ) {
        requireCaller(caller);
        ValidatedCollection validated = validateRequest(caller, request);
        Instant now = Instant.now();
        KnowledgeCollection collection = new KnowledgeCollection(
                "kc-" + UUID.randomUUID(),
                caller.userId(),
                validated.title(),
                validated.description(),
                validated.articleIds(),
                now,
                now
        );
        return detail(collectionRepository.save(collection), caller);
    }

    public KnowledgeCollectionDetailResponse update(
            String collectionId,
            CallerContext caller,
            SaveKnowledgeCollectionRequest request
    ) {
        requireCaller(caller);
        KnowledgeCollection existing = findCollection(collectionId);
        requireEditable(existing, caller);
        ValidatedCollection validated = validateRequest(caller, request);
        KnowledgeCollection updated = new KnowledgeCollection(
                existing.id(),
                existing.ownerId(),
                validated.title(),
                validated.description(),
                validated.articleIds(),
                existing.createdAt(),
                Instant.now()
        );
        return detail(collectionRepository.save(updated), caller);
    }

    public void delete(String collectionId, CallerContext caller) {
        requireCaller(caller);
        KnowledgeCollection existing = findCollection(collectionId);
        requireEditable(existing, caller);
        collectionRepository.deleteById(existing.id());
    }

    private KnowledgeCollectionSummary summary(KnowledgeCollection collection, CallerContext caller) {
        return new KnowledgeCollectionSummary(
                collection.id(),
                collection.ownerId(),
                collection.title(),
                collection.description(),
                visibleArticles(collection, caller).size(),
                collection.createdAt(),
                collection.updatedAt(),
                editable(collection, caller)
        );
    }

    private KnowledgeCollectionDetailResponse detail(KnowledgeCollection collection, CallerContext caller) {
        Map<String, ArticleEngagement> engagement = engagementByArticle();
        List<HomeFeedItem> articles = visibleArticles(collection, caller).stream()
                .map(article -> HomeFeedItemFactory.from(
                        article,
                        engagement.get(article.article().id())
                ))
                .toList();
        return new KnowledgeCollectionDetailResponse(
                collection.id(),
                collection.ownerId(),
                collection.title(),
                collection.description(),
                articles,
                collection.createdAt(),
                collection.updatedAt(),
                editable(collection, caller)
        );
    }

    private List<StoredArticle> visibleArticles(KnowledgeCollection collection, CallerContext caller) {
        return collection.articleIds().stream()
                .map(articleRepository::findById)
                .flatMap(java.util.Optional::stream)
                .filter(article -> article.article().status() == ArticleStatus.PUBLISHED)
                .filter(article -> permissionCheckClient.readAllowed(caller, article.article()))
                .toList();
    }

    private ValidatedCollection validateRequest(
            CallerContext caller,
            SaveKnowledgeCollectionRequest request
    ) {
        if (request == null) {
            throw badRequest("Collection body is required");
        }
        String title = request.title() == null ? "" : request.title().trim();
        if (title.length() < 2 || title.length() > 120) {
            throw badRequest("Collection title must contain 2 to 120 characters");
        }
        String description = request.description() == null ? "" : request.description().trim();
        if (description.length() > 500) {
            throw badRequest("Collection description must not exceed 500 characters");
        }
        List<String> requestedIds = request.articleIds() == null ? List.of() : request.articleIds();
        if (requestedIds.size() < MIN_ARTICLES || requestedIds.size() > MAX_ARTICLES) {
            throw badRequest("A collection must contain 2 to 30 articles");
        }
        Set<String> uniqueIds = new LinkedHashSet<>();
        for (String requestedId : requestedIds) {
            String articleId = requestedId == null ? "" : requestedId.trim();
            if (articleId.isBlank() || articleId.length() > 64 || !uniqueIds.add(articleId)) {
                throw badRequest("Collection article ids must be valid and unique");
            }
            StoredArticle article = articleRepository.findById(articleId)
                    .orElseThrow(() -> badRequest("Only visible published articles can be added"));
            if (article.article().status() != ArticleStatus.PUBLISHED
                    || !permissionCheckClient.readAllowed(caller, article.article())) {
                throw badRequest("Only visible published articles can be added");
            }
        }
        return new ValidatedCollection(title, description, List.copyOf(uniqueIds));
    }

    private Map<String, ArticleEngagement> engagementByArticle() {
        Map<String, ArticleEngagement> result = new LinkedHashMap<>();
        try {
            engagementClient.topArticles(MAX_CANDIDATES)
                    .forEach(item -> result.putIfAbsent(item.articleId(), item));
        } catch (RuntimeException exception) {
            LOGGER.warn("Interaction rankings are unavailable for knowledge collections", exception);
        }
        return result;
    }

    private KnowledgeCollection findCollection(String collectionId) {
        if (collectionId == null || collectionId.isBlank() || collectionId.length() > 64) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge collection not found");
        }
        return collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Knowledge collection not found"
                ));
    }

    private static int normalizeLimit(int requestedLimit) {
        if (requestedLimit <= 0) return DEFAULT_LIMIT;
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private static void requireCaller(CallerContext caller) {
        if (caller == null || caller.userId() == null || caller.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
    }

    private static void requireEditable(KnowledgeCollection collection, CallerContext caller) {
        if (!editable(collection, caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Knowledge collection is not editable");
        }
    }

    private static boolean editable(KnowledgeCollection collection, CallerContext caller) {
        return collection.ownerId().equals(caller.userId()) || caller.roles().contains("ADMIN");
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record ValidatedCollection(String title, String description, List<String> articleIds) {
    }
}
