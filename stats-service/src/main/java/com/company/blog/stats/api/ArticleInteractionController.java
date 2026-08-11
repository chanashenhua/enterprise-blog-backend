package com.company.blog.stats.api;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles/{articleId}/interactions")
public class ArticleInteractionController {
    private final ArticleInteractionService service;

    public ArticleInteractionController(ArticleInteractionService service) {
        this.service = service;
    }

    @GetMapping
    public ArticleInteractionResponse get(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers
    ) {
        return service.get(articleId, headers);
    }

    @PostMapping("/views")
    public ArticleInteractionResponse recordView(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers
    ) {
        return service.recordView(articleId, headers);
    }

    @PutMapping("/likes")
    public ArticleInteractionResponse like(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers
    ) {
        return service.like(articleId, headers);
    }

    @DeleteMapping("/likes")
    public ArticleInteractionResponse unlike(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers
    ) {
        return service.unlike(articleId, headers);
    }

    @PutMapping("/favorites")
    public ArticleInteractionResponse favorite(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers
    ) {
        return service.favorite(articleId, headers);
    }

    @DeleteMapping("/favorites")
    public ArticleInteractionResponse unfavorite(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers
    ) {
        return service.unfavorite(articleId, headers);
    }
}
