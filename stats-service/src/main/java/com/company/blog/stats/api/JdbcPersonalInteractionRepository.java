package com.company.blog.stats.api;

import com.company.blog.stats.InteractionType;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPersonalInteractionRepository implements PersonalInteractionRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcPersonalInteractionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PersonalInteractionItem> findByUser(String userId, InteractionType type, int limit) {
        return jdbcTemplate.query(
                """
                        select article_id, interaction_type, created_at
                        from article_interaction
                        where user_id = ? and interaction_type = ?
                        order by created_at desc, article_id
                        limit ?
                        """,
                (resultSet, rowNumber) -> new PersonalInteractionItem(
                        resultSet.getString("article_id"),
                        resultSet.getString("interaction_type"),
                        resultSet.getTimestamp("created_at").toInstant()
                ),
                userId,
                type.name(),
                limit
        );
    }
}
