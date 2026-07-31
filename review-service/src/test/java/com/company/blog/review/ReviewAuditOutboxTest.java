package com.company.blog.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.review.api.JdbcReviewAuditOutbox;
import com.company.blog.review.api.ReviewAuditOutboxDispatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ReviewAuditOutboxTest {
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:review_audit_outbox;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void persistsAndDeliversReviewAuditEvent() {
        ReviewTicket ticket = new ReviewTicket(
                "ticket-1", "article-1", "request-1", "author-1", "TEAM", Set.of("team-1"),
                ReviewTicketStatus.APPROVED
        );
        new JdbcReviewAuditOutbox(jdbcTemplate).append(
                "reviewer-1", List.of("REVIEWER"), "REVIEW_APPROVE", ticket, null
        );

        List<String> payloads = new ArrayList<>();
        new ReviewAuditOutboxDispatcher(jdbcTemplate, payloads::add, 20, 10).deliverPending();

        assertThat(payloads).singleElement().asString()
                .contains("\"actorId\":\"reviewer-1\"")
                .contains("\"action\":\"REVIEW_APPROVE\"")
                .contains("\"resourceId\":\"article-1\"");
        assertThat(jdbcTemplate.queryForObject(
                "select status from review_audit_event",
                String.class
        )).isEqualTo("DELIVERED");
    }
}
