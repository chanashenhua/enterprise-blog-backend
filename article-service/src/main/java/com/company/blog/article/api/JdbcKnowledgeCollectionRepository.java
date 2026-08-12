package com.company.blog.article.api;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcKnowledgeCollectionRepository implements KnowledgeCollectionRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcKnowledgeCollectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public KnowledgeCollection save(KnowledgeCollection collection) {
        int updated = jdbcTemplate.update(
                """
                        update knowledge_collection
                        set owner_id = ?, title = ?, description = ?, created_at = ?, updated_at = ?
                        where id = ?
                        """,
                collection.ownerId(),
                collection.title(),
                collection.description(),
                Timestamp.from(collection.createdAt()),
                Timestamp.from(collection.updatedAt()),
                collection.id()
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    """
                            insert into knowledge_collection
                                (id, owner_id, title, description, created_at, updated_at)
                            values (?, ?, ?, ?, ?, ?)
                            """,
                    collection.id(),
                    collection.ownerId(),
                    collection.title(),
                    collection.description(),
                    Timestamp.from(collection.createdAt()),
                    Timestamp.from(collection.updatedAt())
            );
        }

        jdbcTemplate.update(
                "delete from knowledge_collection_article where collection_id = ?",
                collection.id()
        );
        for (int position = 0; position < collection.articleIds().size(); position++) {
            jdbcTemplate.update(
                    """
                            insert into knowledge_collection_article (collection_id, article_id, position)
                            values (?, ?, ?)
                            """,
                    collection.id(),
                    collection.articleIds().get(position),
                    position
            );
        }
        return collection;
    }

    @Override
    public Optional<KnowledgeCollection> findById(String collectionId) {
        List<KnowledgeCollectionRow> rows = jdbcTemplate.query(
                """
                        select id, owner_id, title, description, created_at, updated_at
                        from knowledge_collection
                        where id = ?
                        """,
                (resultSet, rowNumber) -> new KnowledgeCollectionRow(
                        resultSet.getString("id"),
                        resultSet.getString("owner_id"),
                        resultSet.getString("title"),
                        resultSet.getString("description"),
                        resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getTimestamp("updated_at").toInstant()
                ),
                collectionId
        );
        if (rows.isEmpty()) return Optional.empty();
        KnowledgeCollectionRow row = rows.get(0);
        List<String> articleIds = jdbcTemplate.queryForList(
                """
                        select article_id
                        from knowledge_collection_article
                        where collection_id = ?
                        order by position
                        """,
                String.class,
                collectionId
        );
        return Optional.of(row.toCollection(articleIds));
    }

    @Override
    public List<KnowledgeCollection> findRecent(int limit) {
        return findIds(
                "select id from knowledge_collection order by updated_at desc, id limit ?",
                limit
        );
    }

    @Override
    public List<KnowledgeCollection> findByOwnerId(String ownerId, int limit) {
        return findIds(
                """
                        select id
                        from knowledge_collection
                        where owner_id = ?
                        order by updated_at desc, id
                        limit ?
                        """,
                ownerId,
                limit
        );
    }

    @Override
    @Transactional
    public boolean deleteById(String collectionId) {
        return jdbcTemplate.update("delete from knowledge_collection where id = ?", collectionId) == 1;
    }

    private List<KnowledgeCollection> findIds(String sql, Object... arguments) {
        return jdbcTemplate.queryForList(sql, String.class, arguments).stream()
                .map(this::findById)
                .flatMap(Optional::stream)
                .toList();
    }

    private record KnowledgeCollectionRow(
            String id,
            String ownerId,
            String title,
            String description,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {
        KnowledgeCollection toCollection(List<String> articleIds) {
            return new KnowledgeCollection(id, ownerId, title, description, articleIds, createdAt, updatedAt);
        }
    }
}
