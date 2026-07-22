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
/**
 * 仅供审核服务调用的内部回调入口。
 *
 * <p>审核完成后由审核服务回调此处推进文章状态，调用方必须携带内部服务令牌，不能通过
 * 公网作者接口直接模拟审核结果。</p>
 */
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
    /** 将匹配当前审核请求的文章发布；重复回调会被领域对象安全地忽略。 */
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
    /** 将匹配当前审核请求的文章退回草稿，保留作者后续修改并再次提交的能力。 */
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
