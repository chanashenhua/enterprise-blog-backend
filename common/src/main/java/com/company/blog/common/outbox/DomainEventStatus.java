package com.company.blog.common.outbox;

public enum DomainEventStatus {
    PENDING,
    DELIVERED,
    FAILED
}