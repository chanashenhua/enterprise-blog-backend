package com.company.blog.permission;

public record PermissionDecision(boolean allowed, String reason) {
    public static PermissionDecision allow() {
        return new PermissionDecision(true, "ALLOWED");
    }

    public static PermissionDecision deny(String reason) {
        return new PermissionDecision(false, reason);
    }
}
