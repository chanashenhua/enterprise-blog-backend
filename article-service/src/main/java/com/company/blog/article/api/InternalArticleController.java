package com.company.blog.article.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/articles")
public class InternalArticleController {
    private final ArticleService articleService;
    private final String reviewToken;

    public InternalArticleController(
            ArticleService articleService,
            @Value("${blog.internal.review-token:local-review-token}") String reviewToken
    ) {
        this.articleService = articleService;
        this.reviewToken = reviewToken;
    }

    @PostMapping("/{articleId}/review-approved")
    public ArticleResponse approveFromReview(
            @PathVariable("articleId") String articleId,
            @RequestHeader("X-Internal-Token") String token,
            @RequestHeader("X-Review-Ticket-Id") String reviewTicketId,
            @RequestHeader("X-Review-Request-Id") String reviewRequestId
    ) {
        requireReviewToken(token);
        return articleService.approveFromReview(articleId, reviewTicketId, reviewRequestId);
    }

    @PostMapping("/{articleId}/review-rejected")
    public ArticleResponse rejectFromReview(
            @PathVariable("articleId") String articleId,
            @RequestHeader("X-Internal-Token") String token,
            @RequestHeader("X-Review-Ticket-Id") String reviewTicketId,
            @RequestHeader("X-Review-Request-Id") String reviewRequestId
    ) {
        requireReviewToken(token);
        return articleService.rejectFromReview(articleId, reviewTicketId, reviewRequestId);
    }

    private void requireReviewToken(String token) {
        if (!reviewToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
    }
}
