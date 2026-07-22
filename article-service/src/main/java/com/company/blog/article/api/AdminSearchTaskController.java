package com.company.blog.article.api;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/search/tasks")
public class AdminSearchTaskController {
    private final JdbcTemplate jdbcTemplate;

    public AdminSearchTaskController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<SearchTaskResponse> list(@RequestHeader HttpHeaders headers) {
        requireAdmin(headers);
        return jdbcTemplate.query(
                "select id, aggregate_id, status, retry_count from domain_event where event_type = 'ArticlePublished' and status = 'FAILED' order by created_at",
                (rs, rowNum) -> new SearchTaskResponse(rs.getString("id"), rs.getString("aggregate_id"), rs.getString("status"), rs.getInt("retry_count"))
        );
    }

    @PostMapping("/{eventId}/retry")
    public void retry(@PathVariable String eventId, @RequestHeader HttpHeaders headers) {
        requireAdmin(headers);
        int changed = jdbcTemplate.update("update domain_event set status = 'PENDING', retry_count = 0, updated_at = current_timestamp where id = ? and event_type = 'ArticlePublished' and status = 'FAILED'", eventId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Retryable search task not found");
    }

    private static void requireAdmin(HttpHeaders headers) {
        String roles = headers.getFirst("X-User-Roles");
        if (roles == null || java.util.Arrays.stream(roles.split(",")).map(String::trim).noneMatch("ADMIN"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role is required");
        }
    }

    public record SearchTaskResponse(String id, String articleId, String status, int retryCount) {
    }
}
