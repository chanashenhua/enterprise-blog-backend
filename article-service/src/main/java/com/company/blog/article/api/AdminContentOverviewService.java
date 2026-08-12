package com.company.blog.article.api;

import java.util.Arrays;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminContentOverviewService {
    private final AdminContentOverviewRepository repository;

    public AdminContentOverviewService(AdminContentOverviewRepository repository) {
        this.repository = repository;
    }

    public ContentOperationsOverview overview(HttpHeaders headers) {
        requireAdmin(headers);
        return repository.overview();
    }

    private static void requireAdmin(HttpHeaders headers) {
        String roles = headers.getFirst("X-User-Roles");
        boolean admin = roles != null && Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch("ADMIN"::equals);
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role is required");
        }
    }
}
