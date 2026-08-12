package com.company.blog.article.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAdminContentOverviewRepository implements AdminContentOverviewRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAdminContentOverviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ContentOperationsOverview overview() {
        return jdbcTemplate.queryForObject(
                """
                        select
                            coalesce(sum(case when status <> 'DELETED' then 1 else 0 end), 0) as total_articles,
                            coalesce(sum(case when status = 'PUBLISHED' then 1 else 0 end), 0) as published_articles,
                            coalesce(sum(case when status = 'DRAFT' then 1 else 0 end), 0) as draft_articles,
                            coalesce(sum(case when status = 'PENDING_REVIEW' then 1 else 0 end), 0) as pending_articles,
                            coalesce(sum(case when status = 'WITHDRAWN' then 1 else 0 end), 0) as withdrawn_articles,
                            coalesce(sum(case when status = 'PUBLISHED' and category_id is not null then 1 else 0 end), 0)
                                as categorized_published,
                            coalesce(sum(case when status = 'PUBLISHED' and exists (
                                select 1 from article_tag where article_tag.article_id = article.id
                            ) then 1 else 0 end), 0) as tagged_published,
                            (select count(*) from knowledge_collection) as collection_count,
                            (select count(*) from knowledge_collection_article) as collection_article_count,
                            (select count(distinct owner_id) from knowledge_collection) as collection_owner_count
                        from article
                        """,
                (resultSet, rowNumber) -> new ContentOperationsOverview(
                        resultSet.getLong("total_articles"),
                        resultSet.getLong("published_articles"),
                        resultSet.getLong("draft_articles"),
                        resultSet.getLong("pending_articles"),
                        resultSet.getLong("withdrawn_articles"),
                        resultSet.getLong("categorized_published"),
                        resultSet.getLong("tagged_published"),
                        resultSet.getLong("collection_count"),
                        resultSet.getLong("collection_article_count"),
                        resultSet.getLong("collection_owner_count")
                )
        );
    }
}
