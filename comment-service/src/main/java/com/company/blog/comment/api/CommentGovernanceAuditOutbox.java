package com.company.blog.comment.api;

import com.company.blog.comment.Comment;
import java.util.List;

public interface CommentGovernanceAuditOutbox {
    void append(String actorId, List<String> actorRoles, String action, Comment comment, String reason);
}
