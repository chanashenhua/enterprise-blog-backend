package com.company.blog.article.api;

import com.company.blog.article.domain.Article;
import com.company.blog.article.domain.ArticleContentProjection;
import com.company.blog.article.domain.ArticleStatus;
import com.company.blog.article.domain.ArticleVisibilityType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 使用 PostgreSQL 保存和恢复完整文章聚合。
 *
 * <p>文章主表保存状态机字段，正文、标签和可见范围分别保存到关联表。所有写语句由上层
 * {@link ArticleTransactionService} 放入同一个事务，防止只保存了部分文章数据。</p>
 */
@Repository
public class JdbcArticleRepository implements ArticleRepository {
    private static final String SELECT_ARTICLE = """
            select id, author_id, title, status, visibility_type, review_request_id,
                   approved_by_review_ticket_id, rejected_by_review_ticket_id,
                   category_id, created_at, updated_at
            from article
            where id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcArticleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(StoredArticle storedArticle) {
        Article article = storedArticle.article();
        int updated = jdbcTemplate.update(
                """
                        update article
                        set author_id = ?, title = ?, status = ?, visibility_type = ?,
                            review_request_id = ?, approved_by_review_ticket_id = ?,
                            rejected_by_review_ticket_id = ?, category_id = ?, created_at = ?, updated_at = ?
                        where id = ?
                        """,
                article.authorId(),
                article.title(),
                article.status().name(),
                visibilityTypeName(article.visibilityType()),
                article.reviewRequestId(),
                article.approvedByReviewTicketId(),
                article.rejectedByReviewTicketId(),
                storedArticle.categoryId(),
                Timestamp.from(article.createdAt()),
                Timestamp.from(article.updatedAt()),
                article.id()
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    """
                            insert into article
                                (id, author_id, title, status, visibility_type, review_request_id,
                                 approved_by_review_ticket_id, rejected_by_review_ticket_id,
                                 category_id, created_at, updated_at)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    article.id(),
                    article.authorId(),
                    article.title(),
                    article.status().name(),
                    visibilityTypeName(article.visibilityType()),
                    article.reviewRequestId(),
                    article.approvedByReviewTicketId(),
                    article.rejectedByReviewTicketId(),
                    storedArticle.categoryId(),
                    Timestamp.from(article.createdAt()),
                    Timestamp.from(article.updatedAt())
            );
        }

        saveContent(storedArticle);
        replaceVisibilityTargets(article);
        replaceTags(storedArticle);
    }

    @Override
    public Optional<StoredArticle> findById(String articleId) {
        return find(articleId, false);
    }

    @Override
    public Optional<StoredArticle> findByIdForUpdate(String articleId) {
        return find(articleId, true);
    }

    @Override
    public List<StoredArticle> findByAuthorId(String authorId) {
        return jdbcTemplate.queryForList(
                        """
                                select id
                                from article
                                where author_id = ? and status <> 'DELETED'
                                order by updated_at desc, id
                                """,
                        String.class,
                        authorId
                ).stream()
                .map(this::findById)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public ArticleContentVersion appendContentVersion(StoredArticle storedArticle, String createdBy) {
        Integer nextVersion = jdbcTemplate.queryForObject(
                """
                        select coalesce(max(version_no), 0) + 1
                        from article_content_version
                        where article_id = ?
                        """,
                Integer.class,
                storedArticle.article().id()
        );
        int versionNo = nextVersion == null ? 1 : nextVersion;
        Instant createdAt = Instant.now();
        jdbcTemplate.update(
                """
                        insert into article_content_version
                            (article_id, version_no, title, content_json, rendered_html,
                             plain_text, tag_ids, category_id, created_by, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                storedArticle.article().id(),
                versionNo,
                storedArticle.article().title(),
                storedArticle.contentJson(),
                storedArticle.content().renderedHtml(),
                storedArticle.content().plainText(),
                commaSeparated(storedArticle.tagIds()),
                storedArticle.categoryId(),
                createdBy,
                Timestamp.from(createdAt)
        );
        return new ArticleContentVersion(
                storedArticle.article().id(),
                versionNo,
                storedArticle.article().title(),
                storedArticle.contentJson(),
                storedArticle.content().renderedHtml(),
                storedArticle.content().plainText(),
                storedArticle.tagIds(),
                storedArticle.categoryId(),
                createdBy,
                createdAt
        );
    }

    @Override
    public List<ArticleContentVersion> findContentVersions(String articleId) {
        return jdbcTemplate.query(
                """
                        select article_id, version_no, title, content_json, rendered_html,
                               plain_text, tag_ids, category_id, created_by, created_at
                        from article_content_version
                        where article_id = ?
                        order by version_no
                        """,
                (resultSet, rowNumber) -> new ArticleContentVersion(
                        resultSet.getString("article_id"),
                        resultSet.getInt("version_no"),
                        resultSet.getString("title"),
                        resultSet.getString("content_json"),
                        resultSet.getString("rendered_html"),
                        resultSet.getString("plain_text"),
                        commaSeparated(resultSet.getString("tag_ids")),
                        resultSet.getString("category_id"),
                        resultSet.getString("created_by"),
                        resultSet.getTimestamp("created_at").toInstant()
                ),
                articleId
        );
    }

    private Optional<StoredArticle> find(String articleId, boolean forUpdate) {
        String sql = forUpdate ? SELECT_ARTICLE + " for update" : SELECT_ARTICLE;
        List<ArticleRow> rows = jdbcTemplate.query(sql, this::mapArticleRow, articleId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        ArticleRow row = rows.get(0);
        ContentRow content = jdbcTemplate.queryForObject(
                """
                        select content_json, rendered_html, plain_text
                        from article_content
                        where article_id = ?
                        """,
                (resultSet, rowNumber) -> new ContentRow(
                        resultSet.getString("content_json"),
                        resultSet.getString("rendered_html"),
                        resultSet.getString("plain_text")
                ),
                articleId
        );
        Set<String> targetIds = new LinkedHashSet<>(jdbcTemplate.queryForList(
                """
                        select target_org_id
                        from article_visibility_target
                        where article_id = ?
                        order by target_org_id
                        """,
                String.class,
                articleId
        ));
        Set<String> tagIds = new LinkedHashSet<>(jdbcTemplate.queryForList(
                """
                        select tag_id
                        from article_tag
                        where article_id = ?
                        order by tag_id
                        """,
                String.class,
                articleId
        ));

        Article article = Article.rehydrate(
                row.id(),
                row.authorId(),
                row.title(),
                row.status(),
                row.visibilityType(),
                targetIds,
                row.reviewRequestId(),
                row.approvedByReviewTicketId(),
                row.rejectedByReviewTicketId(),
                row.createdAt(),
                row.updatedAt()
        );
        return Optional.of(new StoredArticle(
                article,
                content.contentJson(),
                new ArticleContentProjection(content.renderedHtml(), content.plainText()),
                tagIds,
                row.categoryId()
        ));
    }

    private void saveContent(StoredArticle storedArticle) {
        int updated = jdbcTemplate.update(
                """
                        update article_content
                        set content_json = ?, rendered_html = ?, plain_text = ?
                        where article_id = ?
                        """,
                storedArticle.contentJson(),
                storedArticle.content().renderedHtml(),
                storedArticle.content().plainText(),
                storedArticle.article().id()
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    """
                            insert into article_content
                                (article_id, content_json, rendered_html, plain_text)
                            values (?, ?, ?, ?)
                            """,
                    storedArticle.article().id(),
                    storedArticle.contentJson(),
                    storedArticle.content().renderedHtml(),
                    storedArticle.content().plainText()
            );
        }
    }

    private void replaceVisibilityTargets(Article article) {
        jdbcTemplate.update(
                "delete from article_visibility_target where article_id = ?",
                article.id()
        );
        if (article.visibilityType() == null) {
            return;
        }
        article.visibilityTargetIds().stream().sorted().forEach(targetId -> jdbcTemplate.update(
                """
                        insert into article_visibility_target
                            (article_id, visibility_type, target_org_id)
                        values (?, ?, ?)
                        """,
                article.id(),
                article.visibilityType().name(),
                targetId
        ));
    }

    private void replaceTags(StoredArticle storedArticle) {
        jdbcTemplate.update("delete from article_tag where article_id = ?", storedArticle.article().id());
        storedArticle.tagIds().stream().sorted().forEach(tagId -> jdbcTemplate.update(
                "insert into article_tag (article_id, tag_id) values (?, ?)",
                storedArticle.article().id(),
                tagId
        ));
    }

    private ArticleRow mapArticleRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ArticleRow(
                resultSet.getString("id"),
                resultSet.getString("author_id"),
                resultSet.getString("title"),
                ArticleStatus.valueOf(resultSet.getString("status")),
                visibilityType(resultSet.getString("visibility_type")),
                resultSet.getString("review_request_id"),
                resultSet.getString("approved_by_review_ticket_id"),
                resultSet.getString("rejected_by_review_ticket_id"),
                resultSet.getString("category_id"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private static String visibilityTypeName(ArticleVisibilityType visibilityType) {
        return visibilityType == null ? null : visibilityType.name();
    }

    private static ArticleVisibilityType visibilityType(String value) {
        return value == null ? null : ArticleVisibilityType.valueOf(value);
    }

    private static String commaSeparated(Set<String> values) {
        return values.stream().sorted().collect(Collectors.joining(","));
    }

    private static Set<String> commaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private record ArticleRow(
            String id,
            String authorId,
            String title,
            ArticleStatus status,
            ArticleVisibilityType visibilityType,
            String reviewRequestId,
            String approvedByReviewTicketId,
            String rejectedByReviewTicketId,
            String categoryId,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {
    }

    private record ContentRow(String contentJson, String renderedHtml, String plainText) {
    }
}
