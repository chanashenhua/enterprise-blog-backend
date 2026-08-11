package com.company.blog.comment.api;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {
    private final AdminCommentService service;

    public AdminCommentController(AdminCommentService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public CommentGovernanceOverview overview(@RequestHeader HttpHeaders headers) {
        return service.overview(headers);
    }

    @GetMapping
    public List<AdminCommentRecord> search(
            @RequestHeader HttpHeaders headers,
            @RequestParam(name = "articleId", required = false) String articleId,
            @RequestParam(name = "authorId", required = false) String authorId,
            @RequestParam(name = "status", defaultValue = "ALL") String status,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        return service.search(headers, articleId, authorId, status, limit);
    }

    @PostMapping("/{commentId}/hide")
    public AdminCommentRecord hide(
            @RequestHeader HttpHeaders headers,
            @PathVariable("commentId") String commentId,
            @RequestBody(required = false) CommentGovernanceActionRequest request
    ) {
        return service.hide(headers, commentId, request);
    }

    @PostMapping("/{commentId}/restore")
    public AdminCommentRecord restore(
            @RequestHeader HttpHeaders headers,
            @PathVariable("commentId") String commentId,
            @RequestBody(required = false) CommentGovernanceActionRequest request
    ) {
        return service.restore(headers, commentId, request);
    }
}
