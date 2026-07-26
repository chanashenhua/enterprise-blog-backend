package com.company.blog.comment.api;

import com.company.blog.comment.Comment;
import com.company.blog.comment.CommentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final CommentRepository repository;
    private final ArticleAccessClient articleAccessClient;

    public CommentService(CommentRepository repository, ArticleAccessClient articleAccessClient) {
        this.repository = repository;
        this.articleAccessClient = articleAccessClient;
    }

    @Transactional
    public CommentResponse create(String articleId, CreateCommentRequest request, HttpHeaders headers) {
        requireArticleId(articleId);
        CallerIdentity caller = requireCaller(headers);
        articleAccessClient.requireReadable(articleId, headers);
        String content = requireContent(request == null ? null : request.content());
        String parentId = normalizeParentId(request == null ? null : request.parentId());
        if (parentId != null) {
            Comment parent = requiredComment(parentId);
            if (!articleId.equals(parent.articleId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reply parent belongs to another article");
            }
            if (parent.deleted()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot reply to a deleted comment");
            }
            if (parent.parentId() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only one reply level is supported");
            }
        }

        Instant now = Instant.now();
        Comment saved = repository.save(new Comment(
                UUID.randomUUID().toString(),
                articleId,
                parentId,
                caller.userId(),
                content,
                CommentStatus.ACTIVE,
                now,
                now
        ));
        return CommentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(String articleId, HttpHeaders headers) {
        requireArticleId(articleId);
        requireCaller(headers);
        articleAccessClient.requireReadable(articleId, headers);
        return repository.findByArticleId(articleId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse update(
            String articleId,
            String commentId,
            UpdateCommentRequest request,
            HttpHeaders headers
    ) {
        requireArticleId(articleId);
        CallerIdentity caller = requireCaller(headers);
        articleAccessClient.requireReadable(articleId, headers);
        Comment current = requiredArticleComment(articleId, commentId);
        requireOwnerOrAdmin(current, caller);
        if (current.deleted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted comments cannot be edited");
        }
        String content = requireContent(request == null ? null : request.content());
        Comment updated = repository.updateContent(commentId, content)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Comment is no longer editable"));
        return CommentResponse.from(updated);
    }

    @Transactional
    public void delete(String articleId, String commentId, HttpHeaders headers) {
        requireArticleId(articleId);
        CallerIdentity caller = requireCaller(headers);
        articleAccessClient.requireReadable(articleId, headers);
        Comment current = requiredArticleComment(articleId, commentId);
        requireOwnerOrAdmin(current, caller);
        repository.softDelete(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    private Comment requiredArticleComment(String articleId, String commentId) {
        Comment comment = requiredComment(commentId);
        if (!articleId.equals(comment.articleId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }
        return comment;
    }

    private Comment requiredComment(String commentId) {
        if (commentId == null || commentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment id is required");
        }
        return repository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    private static CallerIdentity requireCaller(HttpHeaders headers) {
        CallerIdentity caller = CallerIdentity.from(headers);
        if (caller.userId() == null || caller.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
        return caller;
    }

    private static void requireOwnerOrAdmin(Comment comment, CallerIdentity caller) {
        if (!comment.authorId().equals(caller.userId()) && !caller.admin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the comment author or an admin may do this");
        }
    }

    private static void requireArticleId(String articleId) {
        if (articleId == null || articleId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article id is required");
        }
    }

    private static String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment content is required");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Comment content must not exceed " + MAX_CONTENT_LENGTH + " characters"
            );
        }
        return normalized;
    }

    private static String normalizeParentId(String parentId) {
        return parentId == null || parentId.isBlank() ? null : parentId.trim();
    }
}
