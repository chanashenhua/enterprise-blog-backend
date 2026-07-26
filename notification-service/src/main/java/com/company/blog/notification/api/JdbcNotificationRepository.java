package com.company.blog.notification.api;

import com.company.blog.notification.Notification;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL 通知仓储。
 *
 * <p>event_id 唯一约束吸收 Outbox 的至少一次投递，确保上游重试不会给员工生成重复通知。</p>
 */
@Repository
public class JdbcNotificationRepository implements NotificationRepository {
    private static final String SELECT_NOTIFICATION = """
            select id, event_id, recipient_user_id, notification_type, title, content,
                   resource_type, resource_id, read_at, created_at
            from user_notification
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcNotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Notification save(Notification notification) {
        try {
            jdbcTemplate.update(
                    """
                            insert into user_notification(
                                id, event_id, recipient_user_id, notification_type, title, content,
                                resource_type, resource_id, read_at, created_at
                            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    notification.id(),
                    notification.eventId(),
                    notification.recipientUserId(),
                    notification.type(),
                    notification.title(),
                    notification.content(),
                    notification.resourceType(),
                    notification.resourceId(),
                    notification.readAt() == null ? null : Timestamp.from(notification.readAt()),
                    Timestamp.from(notification.createdAt())
            );
            return notification;
        } catch (DuplicateKeyException ignored) {
            return findByEventId(notification.eventId()).orElseThrow();
        }
    }

    @Override
    public Optional<Notification> findByEventId(String eventId) {
        return jdbcTemplate.query(
                SELECT_NOTIFICATION + " where event_id = ?",
                (resultSet, rowNumber) -> map(resultSet),
                eventId
        ).stream().findFirst();
    }

    @Override
    public List<Notification> findByRecipient(String recipientUserId, int limit) {
        return jdbcTemplate.query(
                SELECT_NOTIFICATION + " where recipient_user_id = ? order by created_at desc, id desc limit ?",
                (resultSet, rowNumber) -> map(resultSet),
                recipientUserId,
                limit
        );
    }

    @Override
    public long countUnread(String recipientUserId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from user_notification where recipient_user_id = ? and read_at is null",
                Long.class,
                recipientUserId
        );
        return count == null ? 0 : count;
    }

    @Override
    public Optional<Notification> markRead(String id, String recipientUserId) {
        jdbcTemplate.update(
                """
                        update user_notification
                        set read_at = coalesce(read_at, current_timestamp)
                        where id = ? and recipient_user_id = ?
                        """,
                id,
                recipientUserId
        );
        return jdbcTemplate.query(
                SELECT_NOTIFICATION + " where id = ? and recipient_user_id = ?",
                (resultSet, rowNumber) -> map(resultSet),
                id,
                recipientUserId
        ).stream().findFirst();
    }

    @Override
    public int markAllRead(String recipientUserId) {
        return jdbcTemplate.update(
                """
                        update user_notification
                        set read_at = current_timestamp
                        where recipient_user_id = ? and read_at is null
                        """,
                recipientUserId
        );
    }

    private static Notification map(ResultSet resultSet) throws SQLException {
        Timestamp readAt = resultSet.getTimestamp("read_at");
        return new Notification(
                resultSet.getString("id"),
                resultSet.getString("event_id"),
                resultSet.getString("recipient_user_id"),
                resultSet.getString("notification_type"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getString("resource_type"),
                resultSet.getString("resource_id"),
                readAt == null ? null : readAt.toInstant(),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}
