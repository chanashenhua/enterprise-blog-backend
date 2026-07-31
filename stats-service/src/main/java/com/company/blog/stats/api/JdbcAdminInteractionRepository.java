package com.company.blog.stats.api;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAdminInteractionRepository implements AdminInteractionRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAdminInteractionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AdminInteractionOverview overview(int topArticleLimit) {
        Map<String, Object> totals = jdbcTemplate.queryForMap(
                """
                        select
                            sum(case when interaction_type = 'VIEW' then 1 else 0 end) as view_count,
                            sum(case when interaction_type = 'LIKE' then 1 else 0 end) as like_count,
                            sum(case when interaction_type = 'FAVORITE' then 1 else 0 end) as favorite_count,
                            count(distinct article_id) as active_article_count,
                            count(distinct user_id) as engaged_user_count
                        from article_interaction
                        """
        );
        List<ArticleInteractionRanking> topArticles = jdbcTemplate.query(
                """
                        select article_id,
                               sum(case when interaction_type = 'VIEW' then 1 else 0 end) as view_count,
                               sum(case when interaction_type = 'LIKE' then 1 else 0 end) as like_count,
                               sum(case when interaction_type = 'FAVORITE' then 1 else 0 end) as favorite_count
                        from article_interaction
                        group by article_id
                        order by view_count desc, like_count desc, favorite_count desc, article_id
                        limit ?
                        """,
                (resultSet, rowNumber) -> new ArticleInteractionRanking(
                        resultSet.getString("article_id"),
                        resultSet.getLong("view_count"),
                        resultSet.getLong("like_count"),
                        resultSet.getLong("favorite_count")
                ),
                topArticleLimit
        );
        return new AdminInteractionOverview(
                number(totals.get("view_count")),
                number(totals.get("like_count")),
                number(totals.get("favorite_count")),
                number(totals.get("active_article_count")),
                number(totals.get("engaged_user_count")),
                topArticles
        );
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }
}
