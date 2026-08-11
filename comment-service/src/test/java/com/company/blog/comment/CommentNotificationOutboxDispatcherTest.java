package com.company.blog.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.comment.api.CommentNotificationOutboxDispatcher;
import com.company.blog.comment.api.JdbcCommentNotificationOutbox;
import com.company.blog.comment.api.JdbcCommentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class CommentNotificationOutboxDispatcherTest {
    @Test
    void persistsAndDeliversAReplyNotification() {
        JdbcTemplate jdbc = migratedJdbc();
        JdbcCommentRepository comments = new JdbcCommentRepository(jdbc);
        Instant now = Instant.now();
        Comment parent = comments.save(new Comment(
                "c-root", "a-1", null, "u-reader", "Root", CommentStatus.ACTIVE, now, now
        ));
        Comment reply = comments.save(new Comment(
                "c-reply", "a-1", parent.id(), "u-author", "Reply", CommentStatus.ACTIVE, now, now
        ));
        new JdbcCommentNotificationOutbox(jdbc).appendReplyNotification(reply, parent);
        List<String> delivered = new ArrayList<>();
        CommentNotificationOutboxDispatcher dispatcher = new CommentNotificationOutboxDispatcher(
                jdbc,
                (eventId, payloadJson) -> {
                    delivered.add(eventId);
                    assertThat(payloadJson).contains("\"recipientUserId\":\"u-reader\"");
                },
                20,
                5
        );

        dispatcher.deliverPending();

        assertThat(delivered).hasSize(1);
        assertThat(jdbc.queryForObject("select status from comment_notification_event", String.class))
                .isEqualTo("DELIVERED");
    }

    private static JdbcTemplate migratedJdbc() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:comment_notification;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        return new JdbcTemplate(dataSource);
    }
}
