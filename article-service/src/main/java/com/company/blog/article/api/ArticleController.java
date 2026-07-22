package com.company.blog.article.api;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/drafts")
    public ArticleResponse saveDraft(
            @RequestHeader("X-User-Id") String authorId,
            @RequestBody SaveDraftRequest request
    ) {
        return articleService.saveDraft(authorId, request);
    }

    @PostMapping("/{articleId}/submit-publish")
    public ArticleResponse submitForPublish(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers,
            @RequestBody SubmitPublishRequest request
    ) {
        return articleService.submitForPublish(articleId, CallerContext.from(headers), request);
    }

    @GetMapping("/{articleId}")
    public ArticleResponse get(@PathVariable("articleId") String articleId) {
        return articleService.get(articleId);
    }
}