package com.company.blog.comment.api;

import com.company.blog.comment.Comment;
import com.company.blog.comment.CommentStatus;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCommentRepository implements CommentRepository, AdminCommentRepository {
    private static final String SELECT_COMMENT = """
            select id, article_id, parent_id, author_id, content, status, created_at, updated_at
            from blog_comment
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Comment save(Comment comment) {
        jdbcTemplate.update(
                """
                        insert into blog_comment
                            (id, article_id, parent_id, author_id, content, status, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                comment.id(),
                comment.articleId(),
                comment.parentId(),
                comment.authorId(),
                comment.content(),
                comment.status().name(),
                Timestamp.from(comment.createdAt()),
                Timestamp.from(comment.updatedAt())
        );
        return comment;
    }

    @Override
    public Optional<Comment> findById(String id) {
        return jdbcTemplate.query(
                SELECT_COMMENT + " where id = ?",
                (resultSet, rowNumber) -> mapComment(resultSet),
                id
        ).stream().findFirst();
    }

    @Override
    public List<Comment> findByArticleId(String articleId) {
        return jdbcTemplate.query(
                SELECT_COMMENT + " where article_id = ? order by created_at, id",
                (resultSet, rowNumber) -> mapComment(resultSet),
                articleId
        );
    }

    @Override
    public Optional<Comment> updateContent(String id, String content) {
        int changed = jdbcTemplate.update(
                """
                        update blog_comment
                        set content = ?, updated_at = CURRENT_TIMESTAMP
                        where id = ? and status = 'ACTIVE'
                        """,
                content,
                id
        );
        return changed == 1 ? findById(id) : Optional.empty();
    }

    @Override
    public Optional<Comment> softDelete(String id) {
        int changed = jdbcTemplate.update(
                """
                        update blog_comment
                        set content = '', status = 'DELETED', updated_at = CURRENT_TIMESTAMP
                        where id = ? and status = 'ACTIVE'
                        """,
                id
        );
        if (changed == 1) {
            return findById(id);
        }
        return findById(id).filter(Comment::deleted);
    }

    @Override
    public List<Comment> search(AdminCommentQuery query) {
        StringBuilder sql = new StringBuilder(SELECT_COMMENT).append(" where 1 = 1");
        List<Object> arguments = new java.util.ArrayList<>();
        if (query.articleId() != null) {
            sql.append(" and article_id = ?");
            arguments.add(query.articleId());
        }
        if (query.authorId() != null) {
            sql.append(" and author_id = ?");
            arguments.add(query.authorId());
        }
        if (query.status() != null) {
            sql.append(" and status = ?");
            arguments.add(query.status().name());
        }
        sql.append(" order by created_at desc, id desc limit ?");
        arguments.add(query.limit());
        return jdbcTemplate.query(
                sql.toString(),
                (resultSet, rowNumber) -> mapComment(resultSet),
                arguments.toArray()
        );
    }

    @Override
    public Optional<Comment> hide(String commentId) {
        jdbcTemplate.update(
                """
                        update blog_comment
                        set status = 'HIDDEN', updated_at = CURRENT_TIMESTAMP
                        where id = ? and status = 'ACTIVE'
                        """,
                commentId
        );
        return findById(commentId);
    }

    @Override
    public Optional<Comment> restore(String commentId) {
        jdbcTemplate.update(
                """
                        update blog_comment
                        set status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
                        where id = ? and status = 'HIDDEN'
                        """,
                commentId
        );
        return findById(commentId);
    }

    private static Comment mapComment(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new Comment(
                resultSet.getString("id"),
                resultSet.getString("article_id"),
                resultSet.getString("parent_id"),
                resultSet.getString("author_id"),
                resultSet.getString("content"),
                CommentStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }
}
