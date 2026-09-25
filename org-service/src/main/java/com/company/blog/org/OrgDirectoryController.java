package com.company.blog.org;

import com.company.blog.common.security.ArticlePublishScope;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class OrgDirectoryController {
    private final JdbcOrgRepository repository;
    private final String internalToken;

    public OrgDirectoryController(JdbcOrgRepository repository, @Value("${blog.internal.org-token:}") String internalToken) {
        this.repository = repository;
        this.internalToken = internalToken;
    }

    @GetMapping("/api/organizations/publish-options")
    public OrgDirectory publishOptions(@RequestHeader HttpHeaders headers) {
        String userId = headers.getFirst("X-User-Id");
        if (userId == null || userId.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        Set<String> roles = values(headers.getFirst("X-User-Roles"));
        if (!ArticlePublishScope.canWrite(roles)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AUTHOR or ADMIN role is required");
        return repository.directory(roles.contains("ADMIN"), values(headers.getFirst("X-Department-Ids")), values(headers.getFirst("X-Team-Ids")));
    }

    @PostMapping("/internal/organizations/validate-targets")
    public ValidationResponse validate(@RequestHeader(value = "X-Internal-Org-Token", required = false) String token,
                                       @RequestBody ValidationRequest request) {
        if (internalToken.isBlank() || token == null || !MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal org token");
        }
        String error = ArticlePublishScope.validationError(request.visibilityType(), request.targetOrgIds());
        if (error != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        String type = ArticlePublishScope.normalize(request.visibilityType());
        return new ValidationResponse(type.equals("COMPANY") || repository.allExist(type, request.targetOrgIds()));
    }

    private static Set<String> values(String header) {
        if (header == null || header.isBlank()) return Set.of();
        return Arrays.stream(header.split(",")).map(String::trim).filter(value -> !value.isBlank()).collect(Collectors.toUnmodifiableSet());
    }

    public record ValidationRequest(String visibilityType, Set<String> targetOrgIds) {}
    public record ValidationResponse(boolean valid) {}
}
