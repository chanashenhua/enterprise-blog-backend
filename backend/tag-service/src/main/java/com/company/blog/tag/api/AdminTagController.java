package com.company.blog.tag.api;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AdminTagController {
    @GetMapping("/api/admin/tags")
    public List<TagResponse> list(@RequestHeader HttpHeaders headers) {
        requireAdmin(headers);
        return List.of(
                new TagResponse("java", "Java"), new TagResponse("spring-cloud", "Spring Cloud"),
                new TagResponse("redis", "Redis"), new TagResponse("postgresql", "PostgreSQL"),
                new TagResponse("elasticsearch", "Elasticsearch")
        );
    }

    private static void requireAdmin(HttpHeaders headers) {
        String roles = headers.getFirst("X-User-Roles");
        if (roles == null || java.util.Arrays.stream(roles.split(",")).map(String::trim).noneMatch("ADMIN"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role is required");
        }
    }
}
