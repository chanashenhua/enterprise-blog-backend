package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import com.company.blog.article.domain.ArticleStatus;
import com.company.blog.article.domain.ArticleVisibilityType;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
/**
 * 文章用例编排服务。
 *
 * <p>它负责将草稿、权限检查、审核策略和搜索索引事件串成一次业务操作；领域对象
 * {@link Article} 负责状态是否合法，外部服务客户端只负责各自的远程契约。</p>
 */
public class ArticleService {
    private final ArticleRepository repository;
    private final TagValidationClient tagValidationClient;
    private final PermissionCheckClient permissionCheckClient;
    private final ArticleTransactionService transactionService;
    private final ReviewPolicyClient reviewPolicyClient;
    private final ReviewTicketClient reviewTicketClient;

    @Autowired
    public ArticleService(
            ArticleRepository repository,
            TagValidationClient tagValidationClient,
            PermissionCheckClient permissionCheckClient,
            ArticleTransactionService transactionService,
            ReviewPolicyClient reviewPolicyClient,
            ReviewTicketClient reviewTicketClient
    ) {
        this.repository = repository;
        this.tagValidationClient = tagValidationClient;
        this.permissionCheckClient = permissionCheckClient;
        this.transactionService = transactionService;
        this.reviewPolicyClient = reviewPolicyClient;
        this.reviewTicketClient = reviewTicketClient;
    }

    /**
     * 不启动 Spring 容器的单元测试入口，使用同一套事务内业务逻辑但不创建数据库事务代理。
     */
    public ArticleService(
            ArticleRepository repository,
            TagValidationClient tagValidationClient,
            PermissionCheckClient permissionCheckClient,
            ArticleOutbox articleOutbox,
            ReviewPolicyClient reviewPolicyClient,
            ReviewTicketClient reviewTicketClient
    ) {
        this(
                repository,
                tagValidationClient,
                permissionCheckClient,
                new ArticleTransactionService(repository, articleOutbox),
                reviewPolicyClient,
                reviewTicketClient
        );
    }

    public ArticleResponse saveDraft(String authorId, SaveDraftRequest request) {
        requireUserId(authorId);
        validateDraft(request.title(), request.contentJson());
        // 标签由标签服务统一维护，保存前拒绝不存在的标签，避免产生不可检索的脏关联。
        tagValidationClient.validate(request.tagIds());
        Article article = Article.draft(UUID.randomUUID().toString(), authorId, request.title());
        ArticleContentProjection content = ArticleContentProjection.from(request.contentJson());
        StoredArticle storedArticle = new StoredArticle(
                article,
                request.contentJson(),
                content,
                request.tagIds()
        );
        return ArticleResponse.from(transactionService.saveDraft(storedArticle, authorId));
    }

    public ArticleResponse updateDraft(
            String articleId,
            CallerContext callerContext,
            UpdateDraftRequest request
    ) {
        requireUserId(callerContext.userId());
        validateDraft(request.title(), request.contentJson());
        StoredArticle storedArticle = findStoredArticle(articleId);
        permissionCheckClient.requireEditAllowed(callerContext, storedArticle.article());
        tagValidationClient.validate(request.tagIds());
        return ArticleResponse.from(transactionService.updateDraft(
                articleId,
                request.title(),
                request.contentJson(),
                request.tagIds(),
                callerContext.userId()
        ));
    }

    public ArticleResponse submitForPublish(String articleId, CallerContext callerContext, SubmitPublishRequest request) {
        StoredArticle storedArticle = findStoredArticle(articleId);
        Article article = storedArticle.article();
        permissionCheckClient.requirePublishAllowed(callerContext, article, request);
        ArticleVisibilityType visibilityType = parseVisibilityType(request.visibilityType());
        boolean reviewRequired = reviewPolicyClient.reviewRequired(request);
        // 全公司可见文章或不要求审核的范围可直接发布；其余情况先创建审核单。
        if (visibilityType == ArticleVisibilityType.COMPANY || !reviewRequired) {
            return ArticleResponse.from(transactionService.publish(
                    articleId,
                    visibilityType,
                    request.targetOrgIds()
            ));
        }

        StoredArticle pendingArticle = transactionService.requestReview(
                articleId,
                visibilityType,
                request.targetOrgIds()
        );
        reviewTicketClient.createTicket(pendingArticle.article(), request);
        return ArticleResponse.from(pendingArticle);
    }

    public ArticleResponse approveFromReview(String articleId, String reviewTicketId, String reviewRequestId) {
        return ArticleResponse.from(transactionService.approveFromReview(
                articleId,
                reviewTicketId,
                reviewRequestId
        ));
    }

    public ArticleResponse rejectFromReview(String articleId, String reviewTicketId, String reviewRequestId) {
        return ArticleResponse.from(transactionService.rejectFromReview(
                articleId,
                reviewTicketId,
                reviewRequestId
        ));
    }

    public ArticleResponse withdraw(String articleId, CallerContext callerContext) {
        requireUserId(callerContext.userId());
        StoredArticle storedArticle = findStoredArticle(articleId);
        permissionCheckClient.requireWithdrawAllowed(callerContext, storedArticle.article());
        return ArticleResponse.from(transactionService.withdraw(articleId));
    }

    public ArticleResponse delete(String articleId, CallerContext callerContext) {
        requireUserId(callerContext.userId());
        StoredArticle storedArticle = findStoredArticle(articleId);
        permissionCheckClient.requireDeleteAllowed(callerContext, storedArticle.article());
        return ArticleResponse.from(transactionService.delete(articleId));
    }

    public ArticleResponse get(String articleId, CallerContext callerContext) {
        requireUserId(callerContext.userId());
        StoredArticle storedArticle = findStoredArticle(articleId);
        if (storedArticle.article().status() == ArticleStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        if (storedArticle.article().status() == ArticleStatus.PUBLISHED) {
            permissionCheckClient.requireReadAllowed(callerContext, storedArticle.article());
        } else {
            permissionCheckClient.requireEditAllowed(callerContext, storedArticle.article());
        }
        return ArticleResponse.from(storedArticle);
    }

    /**
     * 兼容内部测试和尚未传入完整用户上下文的调用方；正式 HTTP 入口始终使用带上下文版本。
     */
    public ArticleResponse get(String articleId) {
        return ArticleResponse.from(findStoredArticle(articleId));
    }

    public List<ArticleResponse> listMine(CallerContext callerContext) {
        requireUserId(callerContext.userId());
        return repository.findByAuthorId(callerContext.userId()).stream()
                .map(ArticleResponse::from)
                .toList();
    }

    public List<ArticleContentVersion> listVersions(String articleId, CallerContext callerContext) {
        requireUserId(callerContext.userId());
        StoredArticle storedArticle = findStoredArticle(articleId);
        permissionCheckClient.requireEditAllowed(callerContext, storedArticle.article());
        return repository.findContentVersions(articleId);
    }

    private StoredArticle findStoredArticle(String articleId) {
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

    private static void validateDraft(String title, String contentJson) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (contentJson == null || contentJson.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentJson is required");
        }
    }

    private static void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id is required");
        }
    }
}
