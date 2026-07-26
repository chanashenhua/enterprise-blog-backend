package com.company.blog.comment.api;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles/{articleId}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @PathVariable("articleId") String articleId,
            @RequestBody CreateCommentRequest request,
            @RequestHeader HttpHeaders headers
    ) {
        return commentService.create(articleId, request, headers);
    }

    @GetMapping
    public List<CommentResponse> list(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers
    ) {
        return commentService.list(articleId, headers);
    }

    @PutMapping("/{commentId}")
    public CommentResponse update(
            @PathVariable("articleId") String articleId,
            @PathVariable("commentId") String commentId,
            @RequestBody UpdateCommentRequest request,
            @RequestHeader HttpHeaders headers
    ) {
        return commentService.update(articleId, commentId, request, headers);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("articleId") String articleId,
            @PathVariable("commentId") String commentId,
            @RequestHeader HttpHeaders headers
    ) {
        commentService.delete(articleId, commentId, headers);
    }
}
