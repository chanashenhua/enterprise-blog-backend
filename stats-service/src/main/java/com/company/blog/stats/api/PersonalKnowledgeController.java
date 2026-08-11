package com.company.blog.stats.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/knowledge")
public class PersonalKnowledgeController {
    private final PersonalKnowledgeService service;

    public PersonalKnowledgeController(PersonalKnowledgeService service) {
        this.service = service;
    }

    @GetMapping("/favorites")
    public List<PersonalInteractionItem> favorites(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return service.favorites(userId, limit);
    }

    @GetMapping("/recent-views")
    public List<PersonalInteractionItem> recentViews(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return service.recentViews(userId, limit);
    }
}
