package com.company.blog.notification.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAdminNotificationRepository implements AdminNotificationRepository {
    private static final String SELECT_RECORDS = """
            select id, event_id, recipient_user_id, notification_type, title, content,
                   resource_type, resource_id, read_at, created_at
            from user_notification
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAdminNotificationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public NotificationGovernanceOverview overview() {
        Map<String, Object> totals = jdbcTemplate.getJdbcTemplate().queryForMap(
                """
                        select count(*) as total_count,
                               sum(case when read_at is null then 1 else 0 end) as unread_count,
                               sum(case when read_at is not null then 1 else 0 end) as read_count,
                               count(distinct recipient_user_id) as recipient_count
                        from user_notification
                        """
        );
        List<NotificationTypeSummary> types = jdbcTemplate.getJdbcTemplate().query(
                """
                        select notification_type,
                               count(*) as total_count,
                               sum(case when read_at is null then 1 else 0 end) as unread_count
                        from user_notification
                        group by notification_type
                        order by total_count desc, notification_type
                        """,
                (resultSet, rowNumber) -> new NotificationTypeSummary(
                        resultSet.getString("notification_type"),
                        resultSet.getLong("total_count"),
                        resultSet.getLong("unread_count")
                )
        );
        return new NotificationGovernanceOverview(
                number(totals.get("total_count")),
                number(totals.get("unread_count")),
                number(totals.get("read_count")),
                number(totals.get("recipient_count")),
                types
        );
    }

    @Override
    public List<AdminNotificationRecord> search(NotificationGovernanceQuery query) {
        StringBuilder sql = new StringBuilder(SELECT_RECORDS).append(" where 1 = 1");
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.recipientUserId() != null) {
            sql.append(" and recipient_user_id = :recipientUserId");
            parameters.addValue("recipientUserId", query.recipientUserId());
        }
        if (query.type() != null) {
            sql.append(" and notification_type = :type");
            parameters.addValue("type", query.type());
        }
        if (query.read() != null) {
            sql.append(query.read() ? " and read_at is not null" : " and read_at is null");
        }
        sql.append(" order by created_at desc, id desc limit :limit");
        parameters.addValue("limit", query.limit());
        return jdbcTemplate.query(sql.toString(), parameters, (resultSet, rowNumber) -> map(resultSet));
    }

    private static AdminNotificationRecord map(ResultSet resultSet) throws SQLException {
        return new AdminNotificationRecord(
                resultSet.getString("id"),
                resultSet.getString("event_id"),
                resultSet.getString("recipient_user_id"),
                resultSet.getString("notification_type"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getString("resource_type"),
                resultSet.getString("resource_id"),
                resultSet.getTimestamp("read_at") != null,
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }
}
