package com.company.blog.article.domain;

import java.time.Instant;
import java.util.Objects;

public record DomainEvent(String type, String aggregateId, Instant occurredAt) {
    public DomainEvent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public static DomainEvent articlePublished(String articleId) {
        return new DomainEvent("ArticlePublished", articleId, Instant.now());
    }

    public static DomainEvent articleWithdrawn(String articleId) {
        return new DomainEvent("ArticleWithdrawn", articleId, Instant.now());
    }

    public static DomainEvent articleDeleted(String articleId) {
        return new DomainEvent("ArticleDeleted", articleId, Instant.now());
    }
}
