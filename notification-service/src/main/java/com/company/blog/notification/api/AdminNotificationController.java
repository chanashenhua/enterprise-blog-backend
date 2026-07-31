package com.company.blog.notification.api;

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
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {
    private final NotificationGovernanceService service;

    public AdminNotificationController(NotificationGovernanceService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public NotificationGovernanceOverview overview(@RequestHeader HttpHeaders headers) {
        requireAdmin(headers);
        return service.overview();
    }

    @GetMapping
    public List<AdminNotificationRecord> search(
            @RequestHeader HttpHeaders headers,
            @RequestParam(name = "recipientUserId", required = false) String recipientUserId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "state", defaultValue = "ALL") String state,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        requireAdmin(headers);
        return service.search(recipientUserId, type, state, limit);
    }

    private static void requireAdmin(HttpHeaders headers) {
        String roles = headers.getFirst("X-User-Roles");
        if (roles == null || Arrays.stream(roles.split(",")).map(String::trim).noneMatch("ADMIN"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role is required");
        }
    }
}
