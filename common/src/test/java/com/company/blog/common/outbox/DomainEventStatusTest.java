package com.company.blog.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainEventStatusTest {
    @Test
    void exposesOutboxDeliveryLifecycleValues() {
        assertThat(DomainEventStatus.values())
                .containsExactly(DomainEventStatus.PENDING, DomainEventStatus.DELIVERED, DomainEventStatus.FAILED);
    }
}