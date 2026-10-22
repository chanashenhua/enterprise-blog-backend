package com.company.blog.article.api;

import com.company.blog.article.domain.ArticleStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ArticleSearchAccessController {
    private final ArticleRepository repository;
    private final PermissionCheckClient permissions;
    private final String token;

    public ArticleSearchAccessController(ArticleRepository repository, PermissionCheckClient permissions,
            @Value("${blog.internal.search-token:local-search-token}") String token) {
        this.repository = repository;
        this.permissions = permissions;
        this.token = token;
    }

    @PostMapping("/internal/articles/search-access")
    public List<String> readableIds(@RequestHeader("X-Internal-Token") String suppliedToken,
            @RequestHeader HttpHeaders headers, @RequestBody List<Candidate> candidates) {
        if (token.isBlank() || !token.equals(suppliedToken)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        CallerContext caller = CallerContext.from(headers);
        if (caller.userId() == null || caller.userId().isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (candidates == null || candidates.size() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        List<String> allowed = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate == null || candidate.articleId() == null || candidate.revision() < 1) continue;
            StoredArticle current = repository.findById(candidate.articleId()).orElse(null);
            if (current == null || current.article().status() != ArticleStatus.PUBLISHED || current.revision() != candidate.revision()) continue;
            try {
                permissions.requireReadAllowed(caller, current.article());
                allowed.add(candidate.articleId());
            } catch (ResponseStatusException denied) {
                if (denied.getStatusCode().value() != 403) throw denied;
            }
        }
        return allowed;
    }

    public record Candidate(String articleId, long revision) { }
}
