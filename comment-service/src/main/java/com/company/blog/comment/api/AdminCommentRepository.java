package com.company.blog.comment.api;

import com.company.blog.comment.Comment;
import java.util.List;
import java.util.Optional;

public interface AdminCommentRepository {
    CommentGovernanceOverview overview();

    List<Comment> search(AdminCommentQuery query);

    Optional<Comment> hide(String commentId);

    Optional<Comment> restore(String commentId);
}
