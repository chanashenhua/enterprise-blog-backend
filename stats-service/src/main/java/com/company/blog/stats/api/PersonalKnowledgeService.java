package com.company.blog.stats.api;

import com.company.blog.stats.InteractionType;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PersonalKnowledgeService {
    private static final int MAX_LIMIT = 100;

    private final PersonalInteractionRepository repository;

    public PersonalKnowledgeService(PersonalInteractionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PersonalInteractionItem> favorites(String userId, int requestedLimit) {
        return find(userId, InteractionType.FAVORITE, requestedLimit);
    }

    @Transactional(readOnly = true)
    public List<PersonalInteractionItem> recentViews(String userId, int requestedLimit) {
        return find(userId, InteractionType.VIEW, requestedLimit);
    }

    private List<PersonalInteractionItem> find(String userId, InteractionType type, int requestedLimit) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
        int limit = Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
        return repository.findByUser(userId.trim(), type, limit);
    }
}
