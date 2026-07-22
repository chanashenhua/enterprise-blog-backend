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
public class ReviewController {
    private final ReviewPolicy reviewPolicy;
    private final ReviewPermissionClient permissionClient;
    private final ArticlePublishCallbackClient articleCallbackClient;
    private final ReviewTicketRepository ticketRepository;
    private final String reviewToken;

    @Autowired
    public ReviewController(
            ReviewPolicy reviewPolicy,
            ReviewPermissionClient permissionClient,
            ArticlePublishCallbackClient articleCallbackClient,
            ReviewTicketRepository ticketRepository,
            @Value("${blog.internal.review-token:local-review-token}") String reviewToken
    ) {
        this.reviewPolicy = reviewPolicy;
        this.permissionClient = permissionClient;
        this.articleCallbackClient = articleCallbackClient;
        this.ticketRepository = ticketRepository;
        this.reviewToken = reviewToken;
    }

    public ReviewController(
            ReviewPolicy reviewPolicy,
            ReviewPermissionClient permissionClient,
            ArticlePublishCallbackClient articleCallbackClient,
            ReviewTicketRepository ticketRepository
    ) {
        this(reviewPolicy, permissionClient, articleCallbackClient, ticketRepository, "local-review-token");
    }

    @PostMapping("/internal/reviews/policies/evaluate")
    public EvaluateReviewPolicyResponse evaluate(
            @RequestBody EvaluateReviewPolicyRequest request,
            @RequestHeader("X-Internal-Token") String token
    ) {
        requireInternalToken(token);
        return reviewPolicy.evaluate(request);
    }

    @PostMapping("/internal/reviews/tickets")
    public ReviewTicketResponse createTicket(
            @RequestBody CreateReviewTicketRequest request,
            @RequestHeader("X-Internal-Token") String token
    ) {
        requireInternalToken(token);
        ReviewTicket ticket = ticketRepository.findOrCreateForArticle(ReviewTicketRepository.create(request));
        return ReviewTicketResponse.from(ticket);
    }

    @PostMapping("/api/admin/reviews/{ticketId}/approve")
    public ReviewTicketResponse approve(
            @PathVariable("ticketId") String ticketId,
            @RequestHeader HttpHeaders headers
    ) {
        permissionClient.requireReviewAllowed(headers);
        ReviewTicket current = ticketRepository.beginApproval(ticketId);
        articleCallbackClient.approveArticle(current.articleId(), current.id(), current.reviewRequestId());
        return ReviewTicketResponse.from(ticketRepository.completeApproval(ticketId));
    }

    @GetMapping("/api/admin/reviews")
    public java.util.List<ReviewTicketResponse> pending(@RequestHeader HttpHeaders headers) {
        permissionClient.requireReviewAllowed(headers);
        return ticketRepository.findPending().stream().map(ReviewTicketResponse::from).toList();
    }

    @PostMapping("/api/admin/reviews/{ticketId}/reject")
    public ReviewTicketResponse reject(
            @PathVariable("ticketId") String ticketId,
            @RequestHeader HttpHeaders headers,
            @RequestBody(required = false) ReviewDecisionRequest request
    ) {
        permissionClient.requireReviewAllowed(headers);
        ReviewTicket current = ticketRepository.beginRejection(ticketId);
        ticketRepository.recordRejectionComment(ticketId, request == null ? "" : request.comment());
        articleCallbackClient.rejectArticle(current.articleId(), current.id(), current.reviewRequestId());
        return ReviewTicketResponse.from(ticketRepository.completeRejection(ticketId));
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
