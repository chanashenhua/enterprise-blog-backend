package com.company.blog.permission.api;

public record PermissionCheckResponse(boolean allowed, String reason) {
}
