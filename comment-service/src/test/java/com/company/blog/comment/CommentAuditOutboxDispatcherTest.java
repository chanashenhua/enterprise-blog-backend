package com.company.blog.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.comment.api.CommentAuditOutboxDispatcher;
import com.company.blog.comment.api.JdbcCommentGovernanceAuditOutbox;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class CommentAuditOutboxDispatcherTest {
    @Test
    void deliversGovernanceAuditWithActorActionAndReason() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:comment_audit_dispatcher;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        new JdbcCommentGovernanceAuditOutbox(jdbc).append(
                "u-admin",
                List.of("ADMIN"),
                "COMMENT_HIDE",
                new Comment("c-1", "a-1", null, "u-reader", "内容", CommentStatus.HIDDEN,
                        Instant.now(), Instant.now()),
                "不当内容"
        );
        List<String> payloads = new ArrayList<>();
        CommentAuditOutboxDispatcher dispatcher = new CommentAuditOutboxDispatcher(jdbc, payloads::add, 20, 10);

        dispatcher.deliverPending();

        assertThat(payloads).singleElement().asString()
                .contains("\"actorId\":\"u-admin\"")
                .contains("\"action\":\"COMMENT_HIDE\"")
                .contains("reason=不当内容");
        assertThat(jdbc.queryForObject("select status from comment_audit_event", String.class))
                .isEqualTo("DELIVERED");
    }
}
