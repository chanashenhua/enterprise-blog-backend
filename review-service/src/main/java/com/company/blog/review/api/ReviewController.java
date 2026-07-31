package com.company.blog.review.api;

import com.company.blog.review.ReviewPolicy;
import com.company.blog.review.ReviewTicket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
/**
 * 审核工作流的接口入口。
 *
 * <p>内部接口用于文章服务查询策略和创建审核单；管理员接口用于实际审批。审批状态先持久化为
 * 处理中，再回调文章服务，降低网络重试造成重复发布或重复退回的风险。</p>
 */
public class ReviewController {
    private final ReviewPolicy reviewPolicy;
    private final ReviewPermissionClient permissionClient;
    private final ArticlePublishCallbackClient articleCallbackClient;
    private final ReviewTicketRepository ticketRepository;
    private final ReviewDecisionCompletionService completionService;
    private final String reviewToken;

    @Autowired
    public ReviewController(
            ReviewPolicy reviewPolicy,
            ReviewPermissionClient permissionClient,
            ArticlePublishCallbackClient articleCallbackClient,
            ReviewTicketRepository ticketRepository,
            ReviewDecisionCompletionService completionService,
            @Value("${blog.internal.review-token:local-review-token}") String reviewToken
    ) {
        this.reviewPolicy = reviewPolicy;
        this.permissionClient = permissionClient;
        this.articleCallbackClient = articleCallbackClient;
        this.ticketRepository = ticketRepository;
        this.completionService = completionService;
        this.reviewToken = reviewToken;
    }

    public ReviewController(
            ReviewPolicy reviewPolicy,
            ReviewPermissionClient permissionClient,
            ArticlePublishCallbackClient articleCallbackClient,
            ReviewTicketRepository ticketRepository
    ) {
        this(
                reviewPolicy,
                permissionClient,
                articleCallbackClient,
                ticketRepository,
                new ReviewDecisionCompletionService(ticketRepository, ReviewAuditOutbox.noop()),
                "local-review-token"
        );
    }

    @PostMapping("/internal/reviews/policies/evaluate")
    /** 供文章服务在提交时判断当前可见范围是否必须进入人工审核。 */
    public EvaluateReviewPolicyResponse evaluate(
            @RequestBody EvaluateReviewPolicyRequest request,
            @RequestHeader("X-Internal-Token") String token
    ) {
        requireInternalToken(token);
        return reviewPolicy.evaluate(request);
    }

    @PostMapping("/internal/reviews/tickets")
    /** 创建或返回同一文章、同一审核请求的审核单，支持文章服务的重复请求。 */
    public ReviewTicketResponse createTicket(
            @RequestBody CreateReviewTicketRequest request,
            @RequestHeader("X-Internal-Token") String token
    ) {
        requireInternalToken(token);
        ReviewTicket ticket = ticketRepository.findOrCreateForArticle(ReviewTicketRepository.create(request));
        return ReviewTicketResponse.from(ticket);
    }

    @PostMapping("/api/admin/reviews/{ticketId}/approve")
    /**
     * 审核通过。先将票据置为 APPROVING，再调用文章服务；重复的通过请求会复用处理中状态。
     */
    public ReviewTicketResponse approve(
            @PathVariable("ticketId") String ticketId,
            @RequestHeader HttpHeaders headers
    ) {
        permissionClient.requireReviewAllowed(headers);
        ReviewTicket current = ticketRepository.beginApproval(ticketId);
        articleCallbackClient.approveArticle(current.articleId(), current.id(), current.reviewRequestId());
        return ReviewTicketResponse.from(completionService.completeApproval(ticketId, headers));
    }

    @GetMapping("/api/admin/reviews")
    public java.util.List<ReviewTicketResponse> pending(@RequestHeader HttpHeaders headers) {
        permissionClient.requireReviewAllowed(headers);
        return ticketRepository.findPending().stream().map(ReviewTicketResponse::from).toList();
    }

    @PostMapping("/api/admin/reviews/{ticketId}/reject")
    /** 审核拒绝并记录可选意见，随后通知文章服务退回草稿。 */
    public ReviewTicketResponse reject(
            @PathVariable("ticketId") String ticketId,
            @RequestHeader HttpHeaders headers,
            @RequestBody(required = false) ReviewDecisionRequest request
    ) {
        permissionClient.requireReviewAllowed(headers);
        ReviewTicket current = ticketRepository.beginRejection(ticketId);
        ticketRepository.recordRejectionComment(ticketId, request == null ? "" : request.comment());
        articleCallbackClient.rejectArticle(current.articleId(), current.id(), current.reviewRequestId());
        return ReviewTicketResponse.from(completionService.completeRejection(
                ticketId,
                headers,
                request == null ? "" : request.comment()
        ));
    }

    public ReviewTicketResponse reject(String ticketId, HttpHeaders headers) {
        return reject(ticketId, headers, null);
    }

    private void requireInternalToken(String token) {
        if (!reviewToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
    }
}
