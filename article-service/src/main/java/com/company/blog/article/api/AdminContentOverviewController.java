package com.company.blog.article.api;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content")
public class AdminContentOverviewController {
    private final AdminContentOverviewService service;

    public AdminContentOverviewController(AdminContentOverviewService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public ContentOperationsOverview overview(@RequestHeader HttpHeaders headers) {
        return service.overview(headers);
    }
}
