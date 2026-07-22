package com.company.blog.article.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
        this.id = requireText(id, "id");
        this.authorId = requireText(authorId, "authorId");
        this.title = requireText(title, "title");
        this.status = ArticleStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.visibilityTargetIds = Set.of();
    }

    public static Article draft(String id, String authorId, String title) {
        return new Article(id, authorId, title);
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
