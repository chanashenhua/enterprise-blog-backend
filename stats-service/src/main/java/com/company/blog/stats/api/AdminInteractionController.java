package com.company.blog.stats.api;

import java.util.Arrays;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminInteractionController {
    private final AdminInteractionService service;

    public AdminInteractionController(AdminInteractionService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public AdminInteractionOverview overview(
            @RequestHeader HttpHeaders headers,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        requireAdmin(headers);
        return service.overview(limit);
    }

    private static void requireAdmin(HttpHeaders headers) {
        String roles = headers.getFirst("X-User-Roles");
        if (roles == null || Arrays.stream(roles.split(",")).map(String::trim).noneMatch("ADMIN"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role is required");
        }
    }
}
