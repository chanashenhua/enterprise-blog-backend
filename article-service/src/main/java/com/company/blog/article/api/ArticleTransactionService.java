package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import com.company.blog.article.domain.ArticleStatus;
import com.company.blog.article.domain.ArticleVisibilityType;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 文章数据库事务边界。
 *
 * <p>文章状态、关联数据和 Outbox 必须在同一事务中落库；审核服务的远程调用则由
 * {@link ArticleService} 在事务提交后执行，避免网络失败回滚已经进入待审核的文章。</p>
 */
@Service
public class ArticleTransactionService {
    private final ArticleRepository repository;
    private final ArticleOutbox articleOutbox;

    public ArticleTransactionService(ArticleRepository repository, ArticleOutbox articleOutbox) {
        this.repository = repository;
        this.articleOutbox = articleOutbox;
    }

    @Transactional
    public StoredArticle saveDraft(StoredArticle article) {
        return saveDraft(article, article.article().authorId());
    }

    @Transactional
    public StoredArticle saveDraft(StoredArticle article, String createdBy) {
        return saveDraft(article, createdBy, false);
    }

    @Transactional
    public StoredArticle saveDraft(StoredArticle article, String createdBy, boolean autosave) {
        if (!repository.insertDraftIfAbsent(article)) {
            // INSERT waits for the competing creator to commit. Never overwrite its content on retry.
            StoredArticle current = findForUpdate(article.article().id());
            if (!autosave && current.article().status() == ArticleStatus.DRAFT
                    && sameContent(current, article.article().title(), article.contentJson(), article.tagIds(), article.categoryId())) {
                appendVersionIfChanged(current, createdBy);
            }
            return current;
        }
        if (!autosave) appendVersionIfChanged(article, createdBy);
        return article;
    }

    @Transactional
    public StoredArticle updateDraft(
            String articleId,
            String title,
            String contentJson,
            Set<String> tagIds,
            String categoryId,
            String updatedBy,
            long expectedRevision,
            boolean autosave
    ) {
        StoredArticle current = findForUpdate(articleId);
        requireEditable(current);
        boolean sameContent = sameContent(current, title, contentJson, tagIds, categoryId);
        if (current.revision() != expectedRevision) {
            // A lost response can be retried without overwriting a newer, different draft.
            if (expectedRevision < current.revision() && sameContent && current.article().status() == ArticleStatus.DRAFT) {
                if (!autosave) appendVersionIfChanged(current, updatedBy);
                return current;
            }
            throw conflict();
        }
        if (sameContent && current.article().status() == ArticleStatus.DRAFT) {
            if (!autosave) appendVersionIfChanged(current, updatedBy);
            return current;
        }
        current.article().updateDraft(title);
        StoredArticle updated = new StoredArticle(
                current.article(),
                contentJson,
                ArticleContentProjection.from(contentJson),
                tagIds,
                categoryId,
                current.revision() + 1
        );
        repository.save(updated);
        if (!autosave) appendVersionIfChanged(updated, updatedBy);
        return updated;
    }

    @Transactional
    public StoredArticle publish(
            String articleId,
            ArticleVisibilityType visibilityType,
            Set<String> targetOrgIds,
            long expectedRevision,
            String updatedBy
    ) {
        StoredArticle storedArticle = findForUpdate(articleId);
        Article article = storedArticle.article();
        if (isPublishRetry(storedArticle, ArticleStatus.PUBLISHED, visibilityType, targetOrgIds, expectedRevision)) {
            return storedArticle;
        }
        requirePublishable(storedArticle, expectedRevision);
        appendVersionIfChanged(storedArticle, updatedBy);
        article.submitForPublish(visibilityType, targetOrgIds, false);
        storedArticle = nextRevision(storedArticle);
        repository.save(storedArticle);
        articleOutbox.appendArticleEvents(storedArticle, article.pullEvents());
        return storedArticle;
    }

    @Transactional
    public StoredArticle requestReview(
            String articleId,
            ArticleVisibilityType visibilityType,
            Set<String> targetOrgIds,
            long expectedRevision,
            String updatedBy
    ) {
        StoredArticle storedArticle = findForUpdate(articleId);
        if (isPublishRetry(storedArticle, ArticleStatus.PENDING_REVIEW, visibilityType, targetOrgIds, expectedRevision)) {
            return storedArticle;
        }
        requirePublishable(storedArticle, expectedRevision);
        appendVersionIfChanged(storedArticle, updatedBy);
        storedArticle.article().requestReview(visibilityType, targetOrgIds);
        storedArticle = nextRevision(storedArticle);
        repository.save(storedArticle);
        return storedArticle;
    }

    @Transactional
    public StoredArticle approveFromReview(
            String articleId,
            String reviewTicketId,
            String reviewRequestId
    ) {
        StoredArticle storedArticle = findForUpdate(articleId);
        Article article = storedArticle.article();
        if (article.approveFromReview(reviewTicketId, reviewRequestId)) {
            storedArticle = nextRevision(storedArticle);
            repository.save(storedArticle);
            articleOutbox.appendArticleEvents(storedArticle, article.pullEvents());
            articleOutbox.appendAuthorNotification(
                    storedArticle,
                    "REVIEW_APPROVED",
                    "文章审核已通过",
                    "《" + article.title() + "》已通过审核并发布。"
            );
        }
        return storedArticle;
    }

    @Transactional
    public StoredArticle rejectFromReview(
            String articleId,
            String reviewTicketId,
            String reviewRequestId
    ) {
        StoredArticle storedArticle = findForUpdate(articleId);
        Article article = storedArticle.article();
        if (article.rejectFromReview(reviewTicketId, reviewRequestId)) {
            storedArticle = nextRevision(storedArticle);
            repository.save(storedArticle);
            articleOutbox.appendAuthorNotification(
                    storedArticle,
                    "REVIEW_REJECTED",
                    "文章审核未通过",
                    "《" + article.title() + "》已退回草稿，请修改后重新提交。"
            );
        }
        return storedArticle;
    }

    @Transactional
    public StoredArticle withdraw(String articleId) {
        StoredArticle storedArticle = findForUpdate(articleId);
        Article article = storedArticle.article();
        article.withdraw();
        storedArticle = nextRevision(storedArticle);
        repository.save(storedArticle);
        articleOutbox.appendArticleEvents(storedArticle, article.pullEvents());
        return storedArticle;
    }

    @Transactional
    public StoredArticle delete(String articleId) {
        StoredArticle storedArticle = findForUpdate(articleId);
        Article article = storedArticle.article();
        if (article.delete()) {
            storedArticle = nextRevision(storedArticle);
            repository.save(storedArticle);
            articleOutbox.appendArticleEvents(storedArticle, article.pullEvents());
        }
        return storedArticle;
    }

    private StoredArticle findForUpdate(String articleId) {
        return repository.findByIdForUpdate(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    private static StoredArticle nextRevision(StoredArticle current) {
        return new StoredArticle(current.article(), current.contentJson(), current.content(),
                current.tagIds(), current.categoryId(), current.revision() + 1);
    }

    private static void requireEditable(StoredArticle current) {
        if (current.article().status() != ArticleStatus.DRAFT && current.article().status() != ArticleStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Article is no longer editable");
        }
    }

    private static void requirePublishable(StoredArticle current, long expectedRevision) {
        if (current.revision() != expectedRevision || current.article().status() != ArticleStatus.DRAFT) {
            throw conflict();
        }
    }

    private static boolean isPublishRetry(StoredArticle current, ArticleStatus status,
                                          ArticleVisibilityType visibilityType, Set<String> targets,
                                          long expectedRevision) {
        return current.article().status() == status
                && current.article().visibilityType() == visibilityType
                && current.article().visibilityTargetIds().equals(targets)
                && (current.revision() == expectedRevision + 1 || current.revision() == expectedRevision);
    }

    private static boolean sameContent(StoredArticle current, String title, String contentJson,
                                       Set<String> tags, String category) {
        return current.article().title().equals(title) && current.contentJson().equals(contentJson)
                && current.tagIds().equals(tags) && Objects.equals(current.categoryId(), category);
    }

    private void appendVersionIfChanged(StoredArticle current, String updatedBy) {
        boolean unchanged = repository.findLatestContentVersion(current.article().id())
                .map(version -> version.title().equals(current.article().title())
                        && version.contentJson().equals(current.contentJson())
                        && version.tagIds().equals(current.tagIds())
                        && Objects.equals(version.categoryId(), current.categoryId()))
                .orElse(false);
        if (!unchanged) repository.appendContentVersion(current, updatedBy);
    }

    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Article revision has changed; reload before saving or publishing");
    }
}
