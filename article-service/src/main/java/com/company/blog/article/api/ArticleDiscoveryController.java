package com.company.blog.article.api;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles/discovery")
public class ArticleDiscoveryController {
    private final ArticleDiscoveryService service;

    public ArticleDiscoveryController(ArticleDiscoveryService service) {
        this.service = service;
    }

    @GetMapping
    public ArticleDiscoveryResponse discover(
            @RequestHeader HttpHeaders headers,
            @RequestParam("type") String type,
            @RequestParam("targetId") String targetId,
            @RequestParam(name = "limit", defaultValue = "30") int limit
    ) {
        return service.discover(CallerContext.from(headers), type, targetId, limit);
    }
}
