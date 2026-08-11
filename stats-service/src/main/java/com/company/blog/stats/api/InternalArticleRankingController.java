package com.company.blog.stats.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/stats/article-rankings")
public class InternalArticleRankingController {
    private final AdminInteractionService service;
    private final String feedToken;

    public InternalArticleRankingController(
            AdminInteractionService service,
            @Value("${blog.internal.feed-token:local-feed-token}") String feedToken
    ) {
        this.service = service;
        this.feedToken = feedToken;
    }

    @GetMapping
    public List<ArticleInteractionRanking> rankings(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        if (!feedToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
        return service.overview(limit).topArticles();
    }
}
