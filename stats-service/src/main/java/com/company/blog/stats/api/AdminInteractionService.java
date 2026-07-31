package com.company.blog.stats.api;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminInteractionService {
    private static final int MAX_TOP_ARTICLES = 50;

    private final AdminInteractionRepository repository;

    public AdminInteractionService(AdminInteractionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AdminInteractionOverview overview(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, MAX_TOP_ARTICLES));
        return repository.overview(limit);
    }
}
