package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleVisibilityType;
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
        repository.save(article);
        return article;
    }

    @Transactional
    public StoredArticle publish(
            String articleId,
            ArticleVisibilityType visibilityType,
            Set<String> targetOrgIds
    ) {
        StoredArticle storedArticle = findForUpdate(articleId);
        Article article = storedArticle.article();
        article.submitForPublish(visibilityType, targetOrgIds, false);
        repository.save(storedArticle);
        articleOutbox.appendArticleEvents(storedArticle, article.pullEvents());
        return storedArticle;
    }

    @Transactional
    public StoredArticle requestReview(
            String articleId,
            ArticleVisibilityType visibilityType,
            Set<String> targetOrgIds
    ) {
        StoredArticle storedArticle = findForUpdate(articleId);
        storedArticle.article().requestReview(visibilityType, targetOrgIds);
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
            repository.save(storedArticle);
            articleOutbox.appendArticleEvents(storedArticle, article.pullEvents());
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
        if (storedArticle.article().rejectFromReview(reviewTicketId, reviewRequestId)) {
            repository.save(storedArticle);
        }
        return storedArticle;
    }

    private StoredArticle findForUpdate(String articleId) {
        return repository.findByIdForUpdate(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }
}
