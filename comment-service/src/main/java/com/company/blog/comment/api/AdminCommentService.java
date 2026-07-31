package com.company.blog.comment.api;

import com.company.blog.comment.Comment;
import com.company.blog.comment.CommentStatus;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCommentService {
    private static final int MAX_LIMIT = 200;

    private final CommentRepository commentRepository;
    private final AdminCommentRepository adminRepository;
    private final CommentGovernanceAuditOutbox auditOutbox;

    public AdminCommentService(
            CommentRepository commentRepository,
            AdminCommentRepository adminRepository,
            CommentGovernanceAuditOutbox auditOutbox
    ) {
        this.commentRepository = commentRepository;
        this.adminRepository = adminRepository;
        this.auditOutbox = auditOutbox;
    }

    @Transactional(readOnly = true)
    public List<AdminCommentRecord> search(
            HttpHeaders headers,
            String articleId,
            String authorId,
            String status,
            int requestedLimit
    ) {
        requireAdmin(headers);
        CommentStatus parsedStatus = parseStatus(status);
        AdminCommentQuery query = new AdminCommentQuery(
                normalize(articleId),
                normalize(authorId),
                parsedStatus,
                Math.max(1, Math.min(requestedLimit, MAX_LIMIT))
        );
        return adminRepository.search(query).stream().map(AdminCommentRecord::from).toList();
    }

    @Transactional
    public AdminCommentRecord hide(
            HttpHeaders headers,
            String commentId,
            CommentGovernanceActionRequest request
    ) {
        CallerIdentity caller = requireAdmin(headers);
        Comment current = requiredComment(commentId);
        if (current.deleted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted comments cannot be hidden");
        }
        if (current.hidden()) {
            return AdminCommentRecord.from(current);
        }
        Comment hidden = adminRepository.hide(commentId)
                .filter(Comment::hidden)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Comment is no longer active"));
        auditOutbox.append(caller.userId(), List.copyOf(caller.roles()), "COMMENT_HIDE", hidden, reason(request));
        return AdminCommentRecord.from(hidden);
    }

    @Transactional
    public AdminCommentRecord restore(
            HttpHeaders headers,
            String commentId,
            CommentGovernanceActionRequest request
    ) {
        CallerIdentity caller = requireAdmin(headers);
        Comment current = requiredComment(commentId);
        if (current.deleted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted comments cannot be restored");
        }
        if (current.active()) {
            return AdminCommentRecord.from(current);
        }
        Comment restored = adminRepository.restore(commentId)
                .filter(Comment::active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Comment is no longer hidden"));
        auditOutbox.append(caller.userId(), List.copyOf(caller.roles()), "COMMENT_RESTORE", restored, reason(request));
        return AdminCommentRecord.from(restored);
    }

    private Comment requiredComment(String commentId) {
        if (commentId == null || commentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment id is required");
        }
        return commentRepository.findById(commentId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    private static CallerIdentity requireAdmin(HttpHeaders headers) {
        CallerIdentity caller = CallerIdentity.from(headers);
        if (caller.userId() == null || caller.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
        if (!caller.admin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required");
        }
        return caller;
    }

    private static CommentStatus parseStatus(String value) {
        String normalized = normalize(value);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        try {
            return CommentStatus.valueOf(normalized.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid comment status");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String reason(CommentGovernanceActionRequest request) {
        return request == null ? null : normalize(request.reason());
    }
}
