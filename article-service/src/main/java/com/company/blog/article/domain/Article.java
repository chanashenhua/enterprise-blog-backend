package com.company.blog.article.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 文章聚合根，封装从草稿到发布或审核退回的状态机。
 *
 * <p>外层服务不能直接修改状态：可见范围、审核请求 ID 和领域事件必须随状态转换一起维护，
 * 才能阻止过期审核回调错误地影响后续提交。</p>
 */
public final class Article {
    private final String id;
    private final String authorId;
    private String title;
    private ArticleStatus status;
    private ArticleVisibilityType visibilityType;
    private Set<String> visibilityTargetIds;
    private String reviewRequestId;
    private String approvedByReviewTicketId;
    private String rejectedByReviewTicketId;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<DomainEvent> events = new ArrayList<>();

    private Article(String id, String authorId, String title) {
        this(
                id,
                authorId,
                title,
                ArticleStatus.DRAFT,
                null,
                Set.of(),
                null,
                null,
                null,
                Instant.now(),
                null
        );
        this.updatedAt = this.createdAt;
    }

    private Article(
            String id,
            String authorId,
            String title,
            ArticleStatus status,
            ArticleVisibilityType visibilityType,
            Set<String> visibilityTargetIds,
            String reviewRequestId,
            String approvedByReviewTicketId,
            String rejectedByReviewTicketId,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = requireText(id, "id");
        this.authorId = requireText(authorId, "authorId");
        this.title = requireText(title, "title");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.visibilityType = visibilityType;
        this.visibilityTargetIds = Set.copyOf(
                visibilityTargetIds == null ? Set.of() : new HashSet<>(visibilityTargetIds)
        );
        this.reviewRequestId = reviewRequestId;
        this.approvedByReviewTicketId = approvedByReviewTicketId;
        this.rejectedByReviewTicketId = rejectedByReviewTicketId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = updatedAt;
    }

    public static Article draft(String id, String authorId, String title) {
        return new Article(id, authorId, title);
    }

    /**
     * 从持久化快照恢复文章，不触发新的领域事件。
     */
    public static Article rehydrate(
            String id,
            String authorId,
            String title,
            ArticleStatus status,
            ArticleVisibilityType visibilityType,
            Set<String> visibilityTargetIds,
            String reviewRequestId,
            String approvedByReviewTicketId,
            String rejectedByReviewTicketId,
            Instant createdAt,
            Instant updatedAt
    ) {
        Article article = new Article(
                id,
                authorId,
                title,
                status,
                visibilityType,
                visibilityTargetIds,
                reviewRequestId,
                approvedByReviewTicketId,
                rejectedByReviewTicketId,
                createdAt,
                Objects.requireNonNull(updatedAt, "updatedAt must not be null")
        );
        if (visibilityType != null) {
            article.validateVisibilityTargets();
        }
        return article;
    }

    /**
     * 修改草稿标题。撤回的文章在首次修改时重新进入草稿状态，之后可再次提交发布。
     */
    public void updateDraft(String title) {
        if (status != ArticleStatus.DRAFT && status != ArticleStatus.WITHDRAWN) {
            throw new IllegalStateException("Only draft or withdrawn articles can be edited");
        }
        this.title = requireText(title, "title");
        if (status == ArticleStatus.WITHDRAWN) {
            this.status = ArticleStatus.DRAFT;
            this.visibilityType = null;
            this.visibilityTargetIds = Set.of();
            this.reviewRequestId = null;
            this.approvedByReviewTicketId = null;
            this.rejectedByReviewTicketId = null;
        }
        this.updatedAt = Instant.now();
    }

    public void withdraw() {
        if (status != ArticleStatus.PUBLISHED) {
            throw new IllegalStateException("Only published articles can be withdrawn");
        }
        this.status = ArticleStatus.WITHDRAWN;
        this.events.add(DomainEvent.articleWithdrawn(id));
        this.updatedAt = Instant.now();
    }

    public boolean delete() {
        if (status == ArticleStatus.DELETED) {
            return false;
        }
        if (status == ArticleStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Pending review articles cannot be deleted");
        }
        this.status = ArticleStatus.DELETED;
        this.events.add(DomainEvent.articleDeleted(id));
        this.updatedAt = Instant.now();
        return true;
    }

    public void submitForPublish(
            ArticleVisibilityType visibilityType,
            Set<String> targetOrgIds,
            boolean reviewRequired
    ) {
        prepareVisibility(visibilityType, targetOrgIds);
        if (visibilityType == ArticleVisibilityType.COMPANY || !reviewRequired) {
            publish();
        } else {
            markPendingReview();
        }
    }

    public static void validateVisibility(ArticleVisibilityType visibilityType, Set<String> targetOrgIds) {
        Set<String> targets = Set.copyOf(targetOrgIds == null ? Set.of() : new HashSet<>(targetOrgIds));
        if (visibilityType == ArticleVisibilityType.COMPANY && !targets.isEmpty()) {
            throw new IllegalArgumentException("Company-visible articles must not have organization targets");
        }
        if ((visibilityType == ArticleVisibilityType.DEPARTMENT || visibilityType == ArticleVisibilityType.TEAM)
                && targets.isEmpty()) {
            throw new IllegalArgumentException("Scoped articles must have at least one organization target");
        }
    }

    public void prepareForReview(ArticleVisibilityType visibilityType, Set<String> targetOrgIds) {
        prepareVisibility(visibilityType, targetOrgIds);
    }

    public void markPendingReview() {
        if (status != ArticleStatus.DRAFT) {
            throw new IllegalStateException("Only draft articles can be submitted for review");
        }
        if (reviewRequestId == null) {
            reviewRequestId = UUID.randomUUID().toString();
        }
        this.status = ArticleStatus.PENDING_REVIEW;
        this.updatedAt = Instant.now();
    }

    public void requestReview(ArticleVisibilityType visibilityType, Set<String> targetOrgIds) {
        Set<String> requestedTargets = Set.copyOf(targetOrgIds == null ? Set.of() : new HashSet<>(targetOrgIds));
        // 同一份待审内容可安全重试；若试图改变范围，必须先回到草稿后重新提交。
        if (status == ArticleStatus.PENDING_REVIEW) {
            if (this.visibilityType == visibilityType && this.visibilityTargetIds.equals(requestedTargets)) {
                return;
            }
            throw new IllegalStateException("Pending review articles cannot change visibility");
        }
        prepareVisibility(visibilityType, requestedTargets);
        this.reviewRequestId = UUID.randomUUID().toString();
        this.rejectedByReviewTicketId = null;
        markPendingReview();
    }

    public boolean approveFromReview(String reviewTicketId, String callbackReviewRequestId) {
        String approvedTicketId = requireText(reviewTicketId, "reviewTicketId");
        // 审核请求 ID 是回调幂等键，也隔离了旧审核单的迟到消息。
        if (!isCurrentReviewRequest(callbackReviewRequestId)) {
            return false;
        }
        if (status == ArticleStatus.PUBLISHED) {
            if (approvedTicketId.equals(approvedByReviewTicketId)) {
                return false;
            }
            return false;
        }
        if (status != ArticleStatus.PENDING_REVIEW) {
            return false;
        }
        this.approvedByReviewTicketId = approvedTicketId;
        publish();
        return true;
    }

    public boolean rejectFromReview(String reviewTicketId, String callbackReviewRequestId) {
        String rejectedTicketId = requireText(reviewTicketId, "reviewTicketId");
        if (!isCurrentReviewRequest(callbackReviewRequestId)) {
            return false;
        }
        if (status == ArticleStatus.DRAFT) {
            if (rejectedTicketId.equals(rejectedByReviewTicketId)) {
                return false;
            }
            return false;
        }
        if (status != ArticleStatus.PENDING_REVIEW) {
            return false;
        }
        this.rejectedByReviewTicketId = rejectedTicketId;
        this.visibilityType = null;
        this.visibilityTargetIds = Set.of();
        this.status = ArticleStatus.DRAFT;
        this.updatedAt = Instant.now();
        return true;
    }

    public List<DomainEvent> pullEvents() {
        /**
         * 取走本次状态转换产生的事件。
         * 调用者完成 Outbox 写入后事件列表会清空，避免同一进程内重复发送。
         */
        List<DomainEvent> pendingEvents = List.copyOf(events);
        events.clear();
        return pendingEvents;
    }

    public String id() {
        return id;
    }

    public String authorId() {
        return authorId;
    }

    public String title() {
        return title;
    }

    public ArticleStatus status() {
        return status;
    }

    public ArticleVisibilityType visibilityType() {
        return visibilityType;
    }

    public Set<String> visibilityTargetIds() {
        return visibilityTargetIds;
    }

    public String reviewRequestId() {
        return reviewRequestId;
    }

    public String approvedByReviewTicketId() {
        return approvedByReviewTicketId;
    }

    public String rejectedByReviewTicketId() {
        return rejectedByReviewTicketId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private void prepareVisibility(ArticleVisibilityType visibilityType, Set<String> targetOrgIds) {
        if (status != ArticleStatus.DRAFT) {
            throw new IllegalStateException("Only draft articles can be submitted for publish");
        }
        this.visibilityType = Objects.requireNonNull(visibilityType, "visibilityType must not be null");
        this.visibilityTargetIds = Set.copyOf(targetOrgIds == null ? Set.of() : new HashSet<>(targetOrgIds));
        validateVisibilityTargets();
    }

    private void publish() {
        this.status = ArticleStatus.PUBLISHED;
        this.events.add(DomainEvent.articlePublished(id));
        this.updatedAt = Instant.now();
    }

    private void validateVisibilityTargets() {
        if (visibilityType == ArticleVisibilityType.COMPANY && !visibilityTargetIds.isEmpty()) {
            throw new IllegalArgumentException("Company-visible articles must not have organization targets");
        }
        if ((visibilityType == ArticleVisibilityType.DEPARTMENT || visibilityType == ArticleVisibilityType.TEAM)
                && visibilityTargetIds.isEmpty()) {
            throw new IllegalArgumentException("Scoped articles must have at least one organization target");
        }
    }

    private boolean isCurrentReviewRequest(String callbackReviewRequestId) {
        return callbackReviewRequestId != null && callbackReviewRequestId.equals(reviewRequestId);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
