package com.company.blog.audit.api;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/audits")
public class AuditController {
    private final AuditApplicationService service;

    public AuditController(AuditApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditRecordResponse> search(
            @RequestHeader HttpHeaders headers,
            @RequestParam(name = "actorId", required = false) String actorId,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "resourceType", required = false) String resourceType,
            @RequestParam(name = "resourceId", required = false) String resourceId,
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        requireAdmin(headers);
        return service.search(new AuditQuery(actorId, action, resourceType, resourceId, from, to, limit));
    }

    private static void requireAdmin(HttpHeaders headers) {
        String roles = headers.getFirst("X-User-Roles");
        if (roles == null || Arrays.stream(roles.split(",")).map(String::trim).noneMatch("ADMIN"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role is required");
        }
    }
}
