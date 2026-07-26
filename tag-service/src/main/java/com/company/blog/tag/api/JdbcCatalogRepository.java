package com.company.blog.tag.api;

import com.company.blog.tag.CatalogItem;
import com.company.blog.tag.CatalogType;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCatalogRepository implements CatalogRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CatalogItem> findAll(CatalogType type, boolean includeInactive) {
        String where = includeInactive ? "" : " where active = true";
        return jdbcTemplate.query(
                select(type) + where + " order by name, id",
                (resultSet, rowNumber) -> map(resultSet)
        );
    }

    @Override
    public Optional<CatalogItem> findById(CatalogType type, String id) {
        return jdbcTemplate.query(
                select(type) + " where id = ?",
                (resultSet, rowNumber) -> map(resultSet),
                id
        ).stream().findFirst();
    }

    @Override
    public CatalogItem create(CatalogType type, String id, String name) {
        jdbcTemplate.update(
                "insert into " + type.tableName() + " (id, name) values (?, ?)",
                id,
                name
        );
        return findById(type, id).orElseThrow();
    }

    @Override
    public Optional<CatalogItem> update(CatalogType type, String id, String name) {
        int changed = jdbcTemplate.update(
                "update " + type.tableName()
                        + " set name = ?, active = true, updated_at = CURRENT_TIMESTAMP where id = ?",
                name,
                id
        );
        return changed == 1 ? findById(type, id) : Optional.empty();
    }

    @Override
    public Optional<CatalogItem> deactivate(CatalogType type, String id) {
        int changed = jdbcTemplate.update(
                "update " + type.tableName()
                        + " set active = false, updated_at = CURRENT_TIMESTAMP where id = ?",
                id
        );
        return changed == 1 ? findById(type, id) : Optional.empty();
    }

    private static String select(CatalogType type) {
        return "select id, name, active, created_at, updated_at from " + type.tableName();
    }

    private static CatalogItem map(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new CatalogItem(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }
}
