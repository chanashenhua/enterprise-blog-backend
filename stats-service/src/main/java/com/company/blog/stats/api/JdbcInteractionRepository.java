package com.company.blog.stats.api;

import com.company.blog.stats.InteractionType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL 互动仓储。
 *
 * <p>数据库主键保证同一用户对同一文章的同类互动只记录一次，因此接口重试不会重复累计。
 * 数量直接从持久化互动事实计算，避免缓存计数与数据库发生漂移。</p>
 */
@Repository
public class JdbcInteractionRepository implements InteractionRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcInteractionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean add(String articleId, String userId, InteractionType type) {
        try {
            return jdbcTemplate.update(
                    """
                            insert into article_interaction(article_id, user_id, interaction_type)
                            values (?, ?, ?)
                            """,
                    articleId,
                    userId,
                    type.name()
            ) == 1;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    @Override
    public boolean remove(String articleId, String userId, InteractionType type) {
        return jdbcTemplate.update(
                """
                        delete from article_interaction
                        where article_id = ? and user_id = ? and interaction_type = ?
                        """,
                articleId,
                userId,
                type.name()
        ) == 1;
    }

    @Override
    public InteractionSnapshot snapshot(String articleId, String userId) {
        return new InteractionSnapshot(
                count(articleId, InteractionType.VIEW),
                count(articleId, InteractionType.LIKE),
                count(articleId, InteractionType.FAVORITE),
                exists(articleId, userId, InteractionType.LIKE),
                exists(articleId, userId, InteractionType.FAVORITE)
        );
    }

    private long count(String articleId, InteractionType type) {
        Long value = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from article_interaction
                        where article_id = ? and interaction_type = ?
                        """,
                Long.class,
                articleId,
                type.name()
        );
        return value == null ? 0 : value;
    }

    private boolean exists(String articleId, String userId, InteractionType type) {
        Long value = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from article_interaction
                        where article_id = ? and user_id = ? and interaction_type = ?
                        """,
                Long.class,
                articleId,
                userId,
                type.name()
        );
        return value != null && value > 0;
    }
}
