package com.company.blog.review.api;

import com.company.blog.review.ReviewTicket;
import com.company.blog.review.ReviewTicketStatus;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ReviewTicketRepository {
    ReviewTicket save(ReviewTicket ticket);

    ReviewTicket findOrCreateForArticle(ReviewTicket ticket);

    Optional<ReviewTicket> findById(String id);

    default List<ReviewTicket> findPending() {
        return List.of();
    }

    ReviewTicket findPendingForTransition(String id);

    ReviewTicket beginApproval(String id);

    ReviewTicket completeApproval(String id);

    ReviewTicket beginRejection(String id);

    ReviewTicket completeRejection(String id);

    ReviewTicket updateStatus(String id, ReviewTicketStatus status);

    default void recordRejectionComment(String id, String comment) {
    }

    static ReviewTicket create(CreateReviewTicketRequest request) {
        return new ReviewTicket(
                UUID.randomUUID().toString(),
                request.articleId(),
                request.reviewRequestId(),
                request.authorId(),
                request.visibilityType(),
                request.targetOrgIds() == null ? Set.of() : request.targetOrgIds(),
                ReviewTicketStatus.PENDING
        );
    }
}
