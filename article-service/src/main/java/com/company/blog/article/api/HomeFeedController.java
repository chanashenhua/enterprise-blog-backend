package com.company.blog.article.api;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles/feed")
public class HomeFeedController {
    private final HomeFeedService service;

    public HomeFeedController(HomeFeedService service) {
        this.service = service;
    }

    @GetMapping
    public HomeFeedResponse home(
            @RequestHeader HttpHeaders headers,
            @RequestParam(name = "limit", defaultValue = "6") int limit
    ) {
        return service.home(CallerContext.from(headers), limit);
    }
}
