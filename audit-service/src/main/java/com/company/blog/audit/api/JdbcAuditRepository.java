package com.company.blog.audit.api;

import com.company.blog.audit.AuditRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuditRepository implements AuditRepository {
    private static final String SELECT_COLUMNS = """
            select id, event_id, source_service, actor_id, actor_roles, action,
                   resource_type, resource_id, outcome, details, trace_id, occurred_at, created_at
            from audit_record
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAuditRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AuditRecord save(AuditRecord record) {
        Optional<AuditRecord> existing = findByEventId(record.eventId());
        if (existing.isPresent()) {
            return existing.get();
        }
        jdbcTemplate.update(
                """
                        insert into audit_record(
                            id, event_id, source_service, actor_id, actor_roles, action,
                            resource_type, resource_id, outcome, details, trace_id, occurred_at, created_at
                        ) values (
                            :id, :eventId, :sourceService, :actorId, :actorRoles, :action,
                            :resourceType, :resourceId, :outcome, :details, :traceId, :occurredAt, :createdAt
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", record.id())
                        .addValue("eventId", record.eventId())
                        .addValue("sourceService", record.sourceService())
                        .addValue("actorId", record.actorId())
                        .addValue("actorRoles", String.join(",", record.actorRoles()))
                        .addValue("action", record.action())
                        .addValue("resourceType", record.resourceType())
                        .addValue("resourceId", record.resourceId())
                        .addValue("outcome", record.outcome())
                        .addValue("details", record.details())
                        .addValue("traceId", record.traceId())
                        .addValue("occurredAt", Timestamp.from(record.occurredAt()))
                        .addValue("createdAt", Timestamp.from(record.createdAt()))
        );
        return record;
    }

    @Override
    public Optional<AuditRecord> findByEventId(String eventId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " where event_id = :eventId",
                new MapSqlParameterSource("eventId", eventId),
                (resultSet, rowNumber) -> map(resultSet)
        ).stream().findFirst();
    }

    @Override
    public List<AuditRecord> search(AuditQuery query) {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS).append(" where 1 = 1");
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        appendFilter(sql, parameters, "actor_id", "actorId", query.actorId());
        appendFilter(sql, parameters, "action", "action", query.action());
        appendFilter(sql, parameters, "resource_type", "resourceType", query.resourceType());
        appendFilter(sql, parameters, "resource_id", "resourceId", query.resourceId());
        if (query.from() != null) {
            sql.append(" and occurred_at >= :from");
            parameters.addValue("from", Timestamp.from(query.from()));
        }
        if (query.to() != null) {
            sql.append(" and occurred_at <= :to");
            parameters.addValue("to", Timestamp.from(query.to()));
        }
        sql.append(" order by occurred_at desc, id desc limit :limit");
        parameters.addValue("limit", query.limit());
        return jdbcTemplate.query(sql.toString(), parameters, (resultSet, rowNumber) -> map(resultSet));
    }

    private static void appendFilter(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            String column,
            String parameter,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            sql.append(" and ").append(column).append(" = :").append(parameter);
            parameters.addValue(parameter, value.trim());
        }
    }

    private static AuditRecord map(ResultSet resultSet) throws SQLException {
        String roles = resultSet.getString("actor_roles");
        return new AuditRecord(
                resultSet.getString("id"),
                resultSet.getString("event_id"),
                resultSet.getString("source_service"),
                resultSet.getString("actor_id"),
                roles == null || roles.isBlank() ? List.of() : Arrays.stream(roles.split(",")).map(String::trim).toList(),
                resultSet.getString("action"),
                resultSet.getString("resource_type"),
                resultSet.getString("resource_id"),
                resultSet.getString("outcome"),
                resultSet.getString("details"),
                resultSet.getString("trace_id"),
                resultSet.getTimestamp("occurred_at").toInstant(),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}
