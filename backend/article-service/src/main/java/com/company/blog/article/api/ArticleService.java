package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import com.company.blog.article.domain.ArticleStatus;
import com.company.blog.article.domain.ArticleVisibilityType;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArticleService {
    private final ArticleMemoryRepository repository;
    private final TagValidationClient tagValidationClient;
    private final PermissionCheckClient permissionCheckClient;
    private final ArticleOutbox articleOutbox;
    private final ReviewPolicyClient reviewPolicyClient;
    private final ReviewTicketClient reviewTicketClient;

    public ArticleService(
            ArticleMemoryRepository repository,
            TagValidationClient tagValidationClient,
            PermissionCheckClient permissionCheckClient,
            ArticleOutbox articleOutbox,
            ReviewPolicyClient reviewPolicyClient,
            ReviewTicketClient reviewTicketClient
    ) {
        this.repository = repository;
        this.tagValidationClient = tagValidationClient;
        this.permissionCheckClient = permissionCheckClient;
        this.articleOutbox = articleOutbox;
        this.reviewPolicyClient = reviewPolicyClient;
        this.reviewTicketClient = reviewTicketClient;
    }

    public ArticleResponse saveDraft(String authorId, SaveDraftRequest request) {
        if (authorId == null || authorId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id is required");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (request.contentJson() == null || request.contentJson().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentJson is required");
        }
        tagValidationClient.validate(request.tagIds());
        Article article = Article.draft(UUID.randomUUID().toString(), authorId, request.title());
        ArticleContentProjection content = ArticleContentProjection.from(request.contentJson());
        ArticleMemoryRepository.StoredArticle storedArticle = new ArticleMemoryRepository.StoredArticle(
                article,
                request.contentJson(),
                content,
                request.tagIds()
        );
        repository.save(storedArticle);
        return ArticleResponse.from(storedArticle);
    }

    public ArticleResponse submitForPublish(String articleId, CallerContext callerContext, SubmitPublishRequest request) {
        ArticleMemoryRepository.StoredArticle storedArticle = findStoredArticle(articleId);
        Article article = storedArticle.article();
        permissionCheckClient.requirePublishAllowed(callerContext, article, request);
        ArticleVisibilityType visibilityType = parseVisibilityType(request.visibilityType());
        boolean reviewRequired = reviewPolicyClient.reviewRequired(request);
        if (visibilityType == ArticleVisibilityType.COMPANY || !reviewRequired) {
            synchronized (storedArticle) {
                article.submitForPublish(visibilityType, request.targetOrgIds(), false);
                articleOutbox.appendArticleEvents(storedArticle, article.pullEvents());
                repository.save(storedArticle);
                return ArticleResponse.from(storedArticle);
            }
        }

        synchronized (storedArticle) {
            article.requestReview(visibilityType, request.targetOrgIds());
            repository.save(storedArticle);
        }
        reviewTicketClient.createTicket(article, request);
        return ArticleResponse.from(storedArticle);
    }

    public ArticleResponse approveFromReview(String articleId, String reviewTicketId, String reviewRequestId) {
        return approveFromReview(findStoredArticle(articleId), reviewTicketId, reviewRequestId);
    }

    private ArticleResponse approveFromReview(
            ArticleMemoryRepository.StoredArticle storedArticle,
            String reviewTicketId,
            String reviewRequestId
    ) {
        synchronized (storedArticle) {
            Article article = storedArticle.article();
            if (article.approveFromReview(reviewTicketId, reviewRequestId)) {
                articleOutbox.appendArticleEvents(storedArticle, article.pullEvents());
                repository.save(storedArticle);
            }
            return ArticleResponse.from(storedArticle);
        }
    }

    public ArticleResponse rejectFromReview(String articleId, String reviewTicketId, String reviewRequestId) {
        return rejectFromReview(findStoredArticle(articleId), reviewTicketId, reviewRequestId);
    }

    private ArticleResponse rejectFromReview(
            ArticleMemoryRepository.StoredArticle storedArticle,
            String reviewTicketId,
            String reviewRequestId
    ) {
        synchronized (storedArticle) {
            Article article = storedArticle.article();
            if (article.rejectFromReview(reviewTicketId, reviewRequestId)) {
                repository.save(storedArticle);
            }
            return ArticleResponse.from(storedArticle);
        }
    }

    public ArticleResponse get(String articleId) {
        return ArticleResponse.from(findStoredArticle(articleId));
    }

    private ArticleMemoryRepository.StoredArticle findStoredArticle(String articleId) {
        return repository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    private static ArticleVisibilityType parseVisibilityType(String visibilityType) {
        if (visibilityType == null || visibilityType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "visibilityType is required");
        }
        try {
            return ArticleVisibilityType.valueOf(visibilityType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported visibilityType", ex);
        }
    }
}
