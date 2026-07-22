package com.company.blog.review.api;

import com.company.blog.review.ReviewTicket;
import com.company.blog.review.ReviewTicketStatus;
import java.util.Arrays;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
/**
 * 基于 PostgreSQL 的审核票据仓储。
 *
 * <p>文章 ID 与审核请求 ID 的唯一约束使创建可重试；条件更新使并发审批只能有一个请求完成状态转换。</p>
 */
public class JdbcReviewTicketRepository implements ReviewTicketRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcReviewTicketRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ReviewTicket save(ReviewTicket ticket) {
        return findOrCreateForArticle(ticket);
    }

    @Override
    public ReviewTicket findOrCreateForArticle(ReviewTicket ticket) {
        try {
            return insert(ticket);
        } catch (DuplicateKeyException ex) {
            // 已创建说明是同一次调用重试，返回已有票据而不是生成第二个审核任务。
            return findByArticleAndReviewRequest(ticket.articleId(), ticket.reviewRequestId()).orElseThrow(() -> ex);
        }
    }

    private ReviewTicket insert(ReviewTicket ticket) {
        jdbcTemplate.update(
                """
                        insert into review_ticket
                            (id, article_id, review_request_id, author_id, visibility_type, target_org_ids, status)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                ticket.id(),
                ticket.articleId(),
                ticket.reviewRequestId(),
                ticket.authorId(),
                ticket.visibilityType(),
                String.join(",", ticket.targetOrgIds()),
                ticket.status().name()
        );
        return ticket;
    }

    private Optional<ReviewTicket> findByArticleAndReviewRequest(String articleId, String reviewRequestId) {
        return jdbcTemplate.query(
                """
                        select id, article_id, review_request_id, author_id, visibility_type, target_org_ids, status
                        from review_ticket
                        where article_id = ? and review_request_id = ?
                        """,
                (rs, rowNum) -> new ReviewTicket(
                        rs.getString("id"),
                        rs.getString("article_id"),
                        rs.getString("review_request_id"),
                        rs.getString("author_id"),
                        rs.getString("visibility_type"),
                        commaSeparated(rs.getString("target_org_ids")),
                        ReviewTicketStatus.valueOf(rs.getString("status"))
                ),
                articleId,
                reviewRequestId
        ).stream().findFirst();
    }

    @Override
    public Optional<ReviewTicket> findById(String id) {
        return jdbcTemplate.query(
                "select id, article_id, review_request_id, author_id, visibility_type, target_org_ids, status from review_ticket where id = ?",
                (rs, rowNum) -> new ReviewTicket(
                        rs.getString("id"),
                        rs.getString("article_id"),
                        rs.getString("review_request_id"),
                        rs.getString("author_id"),
                        rs.getString("visibility_type"),
                        commaSeparated(rs.getString("target_org_ids")),
                        ReviewTicketStatus.valueOf(rs.getString("status"))
                ),
                id
        ).stream().findFirst();
    }

    @Override
    public List<ReviewTicket> findPending() {
        return jdbcTemplate.query(
                "select id, article_id, review_request_id, author_id, visibility_type, target_org_ids, status from review_ticket where status = 'PENDING' order by created_at",
                (rs, rowNum) -> new ReviewTicket(
                        rs.getString("id"), rs.getString("article_id"), rs.getString("review_request_id"),
                        rs.getString("author_id"), rs.getString("visibility_type"), commaSeparated(rs.getString("target_org_ids")),
                        ReviewTicketStatus.valueOf(rs.getString("status"))
                )
        );
    }

    @Override
    public ReviewTicket findPendingForTransition(String id) {
        ReviewTicket current = jdbcTemplate.query(
                "select id, article_id, review_request_id, author_id, visibility_type, target_org_ids, status from review_ticket where id = ? for update",
                (rs, rowNum) -> new ReviewTicket(
                        rs.getString("id"),
                        rs.getString("article_id"),
                        rs.getString("review_request_id"),
                        rs.getString("author_id"),
                        rs.getString("visibility_type"),
                        commaSeparated(rs.getString("target_org_ids")),
                        ReviewTicketStatus.valueOf(rs.getString("status"))
                ),
                id
        ).stream().findFirst().orElseThrow(() -> new ReviewTicketNotFoundException(id));
        if (current.status() != ReviewTicketStatus.PENDING) {
            throw new ReviewTicketStateConflictException(id);
        }
        return current;
    }

    @Override
    public ReviewTicket beginApproval(String id) {
        ReviewTicket current = requiredTicket(id);
        if (current.status() == ReviewTicketStatus.APPROVING) {
            return current;
        }
        if (current.status() != ReviewTicketStatus.PENDING) {
            throw new ReviewTicketStateConflictException(id);
        }
        return transitionFrom(id, ReviewTicketStatus.PENDING, ReviewTicketStatus.APPROVING, true);
    }

    @Override
    public ReviewTicket completeApproval(String id) {
        return transitionFrom(id, ReviewTicketStatus.APPROVING, ReviewTicketStatus.APPROVED, true);
    }

    @Override
    public ReviewTicket beginRejection(String id) {
        ReviewTicket current = requiredTicket(id);
        if (current.status() == ReviewTicketStatus.REJECTING) {
            return current;
        }
        if (current.status() != ReviewTicketStatus.PENDING) {
            throw new ReviewTicketStateConflictException(id);
        }
        return transitionFrom(id, ReviewTicketStatus.PENDING, ReviewTicketStatus.REJECTING, true);
    }

    @Override
    public ReviewTicket completeRejection(String id) {
        return transitionFrom(id, ReviewTicketStatus.REJECTING, ReviewTicketStatus.REJECTED, true);
    }

    @Override
    public ReviewTicket updateStatus(String id, ReviewTicketStatus status) {
        return transitionFrom(id, ReviewTicketStatus.PENDING, status, false);
    }

    @Override
    public void recordRejectionComment(String id, String comment) {
        jdbcTemplate.update("update review_ticket set decision_comment = ? where id = ?", comment, id);
    }

    private ReviewTicket transitionFrom(
            String id,
            ReviewTicketStatus expectedStatus,
            ReviewTicketStatus targetStatus,
            boolean allowExistingTarget
    ) {
        // 将期望旧状态放进 WHERE 条件，相当于一次轻量级乐观并发控制。
        int changed = jdbcTemplate.update(
                """
                        update review_ticket
                        set status = ?, updated_at = CURRENT_TIMESTAMP
                        where id = ? and status = ?
                        """,
                targetStatus.name(),
                id,
                expectedStatus.name()
        );
        if (changed == 1) {
            return requiredTicket(id);
        }
        ReviewTicket current = requiredTicket(id);
        if (allowExistingTarget && current.status() == targetStatus) {
            return current;
        }
        throw new ReviewTicketStateConflictException(id);
    }

    private ReviewTicket requiredTicket(String id) {
        return findById(id).orElseThrow(() -> new ReviewTicketNotFoundException(id));
    }

    private static Set<String> commaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
