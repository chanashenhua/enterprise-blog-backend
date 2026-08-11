package com.company.blog.notification.api;

import java.util.Arrays;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/notifications/subscriptions")
public class AdminSubscriptionController {
    private final SubscriptionGovernanceService service;

    public AdminSubscriptionController(SubscriptionGovernanceService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public SubscriptionGovernanceOverview overview(@RequestHeader HttpHeaders headers) {
        String roles = headers.getFirst("X-User-Roles");
        if (roles == null || Arrays.stream(roles.split(",")).map(String::trim).noneMatch("ADMIN"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role is required");
        }
        return service.overview();
    }
}
