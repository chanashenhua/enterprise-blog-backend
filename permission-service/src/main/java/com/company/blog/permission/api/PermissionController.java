package com.company.blog.permission.api;

import com.company.blog.permission.PermissionDecision;
import com.company.blog.permission.PermissionPolicy;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/permissions")
public class PermissionController {
    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLES_HEADER = "X-User-Roles";
    static final String DEPARTMENT_IDS_HEADER = "X-Department-Ids";
    static final String TEAM_IDS_HEADER = "X-Team-Ids";

    private final PermissionPolicy permissionPolicy;

    public PermissionController(PermissionPolicy permissionPolicy) {
        this.permissionPolicy = permissionPolicy;
    }

    @PostMapping("/check")
    public PermissionCheckResponse check(
            @RequestBody PermissionCheckRequest request,
            @RequestHeader HttpHeaders headers
    ) {
        return check(resolveCallerContext(request, headers));
    }

    PermissionCheckResponse check(PermissionCheckRequest request) {
        PermissionDecision decision = permissionPolicy.check(request);
        return new PermissionCheckResponse(decision.allowed(), decision.reason());
    }

    private static PermissionCheckRequest resolveCallerContext(PermissionCheckRequest request, HttpHeaders headers) {
        return new PermissionCheckRequest(
                firstNonBlank(headers, USER_ID_HEADER).orElse(request.userId()),
                commaSeparatedHeader(headers, USER_ROLES_HEADER).orElse(request.roles()),
                commaSeparatedHeader(headers, DEPARTMENT_IDS_HEADER).orElse(request.departmentIds()),
                commaSeparatedHeader(headers, TEAM_IDS_HEADER).orElse(request.teamIds()),
                request.action(),
                request.resourceType(),
                request.resourceOwnerId(),
                request.visibilityType(),
                request.targetOrgIds()
        );
    }

    private static Optional<String> firstNonBlank(HttpHeaders headers, String headerName) {
        return Optional.ofNullable(headers.getFirst(headerName)).filter(value -> !value.isBlank());
    }

    private static Optional<Set<String>> commaSeparatedHeader(HttpHeaders headers, String headerName) {
        return firstNonBlank(headers, headerName).map(value -> Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet()));
    }
}
