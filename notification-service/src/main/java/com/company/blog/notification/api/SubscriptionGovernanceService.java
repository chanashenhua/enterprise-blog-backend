package com.company.blog.notification.api;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionGovernanceService {
    private final AdminSubscriptionRepository repository;

    public SubscriptionGovernanceService(AdminSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SubscriptionGovernanceOverview overview() {
        return repository.overview();
    }
}
