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
/**
 * 权限服务的内部决策接口。
 *
 * <p>调用者身份优先取网关传来的请求头；请求体中的身份字段只是便于内部任务或测试调用，不能覆盖
 * 已有可信上下文。接口只返回允许与拒绝原因，不泄漏策略实现细节。</p>
 */
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
    /** 根据用户、动作和资源范围给出一次无状态授权决策。 */
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
        // 网关头存在时优先使用，防止服务调用方用请求体冒充另一名用户。
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
