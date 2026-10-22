package com.company.blog.article.api;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/search/tasks")
public class AdminSearchTaskController {
    private static final String SEARCH_TYPES = "('ArticlePublished','ArticleWithdrawn','ArticleDeleted','ArticleSearchSync')";
    private final JdbcTemplate jdbc;

    public AdminSearchTaskController(JdbcTemplate jdbcTemplate) { this.jdbc = jdbcTemplate; }

    @GetMapping
    public List<SearchTaskResponse> list(@RequestHeader HttpHeaders headers,
            @RequestParam(value = "status", defaultValue = "FAILED") String status) {
        requireAdmin(headers);
        if (!java.util.Set.of("FAILED", "PENDING").contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return jdbc.query("select id, aggregate_id, event_type, status, retry_count from domain_event where event_type in "
                        + SEARCH_TYPES + " and status = ? order by created_at, id limit 200",
                (rs, rowNum) -> new SearchTaskResponse(rs.getString("id"), rs.getString("aggregate_id"),
                        rs.getString("event_type"), rs.getString("status"), rs.getInt("retry_count")), status);
    }

    @PostMapping("/{eventId}/retry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retry(@PathVariable("eventId") String eventId, @RequestHeader HttpHeaders headers) {
        requireAdmin(headers);
        int changed = jdbc.update("update domain_event set status = 'PENDING', retry_count = 0, updated_at = current_timestamp "
                + "where id = ? and event_type in " + SEARCH_TYPES + " and status = 'FAILED'", eventId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Retryable search task not found");
    }

    @PostMapping("/reindex")
    @Transactional
    public ReindexResponse reindex(@RequestHeader HttpHeaders headers) {
        requireAdmin(headers);
        // Queue reconciliation for all states, so withdrawn/deleted documents are removed too.
        List<String> ids = jdbc.queryForList("select id from article order by id", String.class);
        int queued = 0;
        for (String id : ids) {
            queued += jdbc.update("""
                    insert into domain_event (id, aggregate_type, aggregate_id, event_type, payload_json, status, retry_count)
                    select ?, 'article', ?, 'ArticleSearchSync', '{}', 'PENDING', 0
                    where not exists (
                        select 1 from domain_event where aggregate_id = ? and event_type = 'ArticleSearchSync' and status = 'PENDING'
                    )
                    """, UUID.randomUUID().toString(), id, id);
        }
        return new ReindexResponse(queued);
    }

    private static void requireAdmin(HttpHeaders headers) {
        String roles = headers.getFirst("X-User-Roles");
        if (headers.getFirst("X-User-Id") == null || roles == null
                || java.util.Arrays.stream(roles.split(",")).map(String::trim).noneMatch("ADMIN"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role is required");
        }
    }

    public record SearchTaskResponse(String id, String articleId, String eventType, String status, int retryCount) { }
    public record ReindexResponse(int queued) { }
}
