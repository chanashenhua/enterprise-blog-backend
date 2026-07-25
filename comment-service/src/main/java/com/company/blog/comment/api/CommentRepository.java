package com.company.blog.comment.api;

import com.company.blog.comment.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment save(Comment comment);

    Optional<Comment> findById(String id);

    List<Comment> findByArticleId(String articleId);

    Optional<Comment> updateContent(String id, String content);

    Optional<Comment> softDelete(String id);
}
