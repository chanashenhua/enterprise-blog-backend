package com.company.blog.notification.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcContentSubscriptionRepository implements ContentSubscriptionRepository, AdminSubscriptionRepository {
    private static final String SELECT = """
            select id, user_id, target_type, target_id, created_at
            from content_subscription
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcContentSubscriptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ContentSubscription save(ContentSubscription subscription) {
        Optional<ContentSubscription> existing = find(
                subscription.userId(), subscription.targetType(), subscription.targetId()
        );
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            jdbcTemplate.update(
                    """
                            insert into content_subscription(id, user_id, target_type, target_id, created_at)
                            values (?, ?, ?, ?, ?)
                            """,
                    subscription.id(),
                    subscription.userId(),
                    subscription.targetType().name(),
                    subscription.targetId(),
                    Timestamp.from(subscription.createdAt())
            );
        } catch (DuplicateKeyException ignored) {
            // 并发重复订阅由数据库唯一约束吸收，随后返回已经存在的关系。
        }
        return find(subscription.userId(), subscription.targetType(), subscription.targetId()).orElseThrow();
    }

    @Override
    public Optional<ContentSubscription> find(
            String userId,
            SubscriptionTargetType targetType,
            String targetId
    ) {
        return jdbcTemplate.query(
                SELECT + " where user_id = ? and target_type = ? and target_id = ?",
                (resultSet, rowNumber) -> map(resultSet),
                userId,
                targetType.name(),
                targetId
        ).stream().findFirst();
    }

    @Override
    public List<ContentSubscription> findByUser(String userId) {
        return jdbcTemplate.query(
                SELECT + " where user_id = ? order by created_at desc, id desc",
                (resultSet, rowNumber) -> map(resultSet),
                userId
        );
    }

    @Override
    public void delete(String userId, SubscriptionTargetType targetType, String targetId) {
        jdbcTemplate.update(
                "delete from content_subscription where user_id = ? and target_type = ? and target_id = ?",
                userId,
                targetType.name(),
                targetId
        );
    }

    @Override
    public Set<String> findSubscriberUserIds(String categoryId, Set<String> tagIds) {
        Set<String> users = new LinkedHashSet<>();
        if (categoryId != null && !categoryId.isBlank()) {
            users.addAll(findUsers(SubscriptionTargetType.CATEGORY, categoryId));
        }
        if (tagIds != null) {
            tagIds.stream().filter(value -> value != null && !value.isBlank())
                    .forEach(tagId -> users.addAll(findUsers(SubscriptionTargetType.TAG, tagId)));
        }
        return users;
    }

    private List<String> findUsers(SubscriptionTargetType type, String targetId) {
        return jdbcTemplate.queryForList(
                "select user_id from content_subscription where target_type = ? and target_id = ? order by user_id",
                String.class,
                type.name(),
                targetId
        );
    }

    @Override
    public SubscriptionGovernanceOverview overview() {
        Map<String, Object> totals = jdbcTemplate.queryForMap(
                """
                        select count(*) as total_count,
                               count(distinct user_id) as subscriber_count,
                               coalesce(sum(case when target_type = 'TAG' then 1 else 0 end), 0) as tag_count,
                               coalesce(sum(case when target_type = 'CATEGORY' then 1 else 0 end), 0) as category_count
                        from content_subscription
                        """
        );
        return new SubscriptionGovernanceOverview(
                number(totals.get("total_count")),
                number(totals.get("subscriber_count")),
                number(totals.get("tag_count")),
                number(totals.get("category_count")),
                topTargets()
        );
    }

    private List<SubscriptionTargetSummary> topTargets() {
        return jdbcTemplate.query(
                """
                        select target_type, target_id, count(distinct user_id) as subscriber_count
                        from content_subscription
                        group by target_type, target_id
                        order by subscriber_count desc, target_type, target_id
                        limit 10
                        """,
                (resultSet, rowNumber) -> new SubscriptionTargetSummary(
                        SubscriptionTargetType.valueOf(resultSet.getString("target_type")),
                        resultSet.getString("target_id"),
                        resultSet.getLong("subscriber_count")
                )
        );
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    private static ContentSubscription map(ResultSet resultSet) throws SQLException {
        return new ContentSubscription(
                resultSet.getString("id"),
                resultSet.getString("user_id"),
                SubscriptionTargetType.valueOf(resultSet.getString("target_type")),
                resultSet.getString("target_id"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}
