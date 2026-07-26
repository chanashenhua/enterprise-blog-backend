package com.company.blog.stats.api;

import com.company.blog.stats.InteractionType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 文章互动用例入口。
 *
 * <p>所有读写都先验证调用者身份与文章可读权限；写操作具有幂等性，重复浏览、重复点赞或重复收藏
 * 不会产生额外记录。</p>
 */
@Service
public class ArticleInteractionService {
    private final InteractionRepository repository;
    private final ArticleAccessClient articleAccessClient;

    public ArticleInteractionService(
            InteractionRepository repository,
            ArticleAccessClient articleAccessClient
    ) {
        this.repository = repository;
        this.articleAccessClient = articleAccessClient;
    }

    @Transactional(readOnly = true)
    public ArticleInteractionResponse get(String articleId, HttpHeaders headers) {
        CallerIdentity caller = requireAccess(articleId, headers);
        return response(articleId, caller.userId());
    }

    @Transactional
    public ArticleInteractionResponse recordView(String articleId, HttpHeaders headers) {
        CallerIdentity caller = requireAccess(articleId, headers);
        repository.add(articleId, caller.userId(), InteractionType.VIEW);
        return response(articleId, caller.userId());
    }

    @Transactional
    public ArticleInteractionResponse like(String articleId, HttpHeaders headers) {
        return add(articleId, headers, InteractionType.LIKE);
    }

    @Transactional
    public ArticleInteractionResponse unlike(String articleId, HttpHeaders headers) {
        return remove(articleId, headers, InteractionType.LIKE);
    }

    @Transactional
    public ArticleInteractionResponse favorite(String articleId, HttpHeaders headers) {
        return add(articleId, headers, InteractionType.FAVORITE);
    }

    @Transactional
    public ArticleInteractionResponse unfavorite(String articleId, HttpHeaders headers) {
        return remove(articleId, headers, InteractionType.FAVORITE);
    }

    private ArticleInteractionResponse add(
            String articleId,
            HttpHeaders headers,
            InteractionType type
    ) {
        CallerIdentity caller = requireAccess(articleId, headers);
        repository.add(articleId, caller.userId(), type);
        return response(articleId, caller.userId());
    }

    private ArticleInteractionResponse remove(
            String articleId,
            HttpHeaders headers,
            InteractionType type
    ) {
        CallerIdentity caller = requireAccess(articleId, headers);
        repository.remove(articleId, caller.userId(), type);
        return response(articleId, caller.userId());
    }

    private CallerIdentity requireAccess(String articleId, HttpHeaders headers) {
        if (articleId == null || articleId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article id is required");
        }
        CallerIdentity caller = CallerIdentity.from(headers);
        if (caller.userId() == null || caller.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
        articleAccessClient.requireReadable(articleId, headers);
        return caller;
    }

    private ArticleInteractionResponse response(String articleId, String userId) {
        return ArticleInteractionResponse.from(articleId, repository.snapshot(articleId, userId));
    }
}
