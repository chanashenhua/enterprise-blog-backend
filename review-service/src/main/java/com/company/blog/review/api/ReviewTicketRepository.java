package com.company.blog.review.api;

import com.company.blog.review.ReviewTicket;
import com.company.blog.review.ReviewTicketStatus;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 审核票据的持久化边界。
 *
 * <p>审批相关方法按阶段命名，调用者只能沿 PENDING -> 处理中 -> 最终状态转换，不能任意覆盖状态。</p>
 */
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
        // 票据 ID 与文章审核请求 ID 分开：前者定位工作项，后者定位一次发布尝试。
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
