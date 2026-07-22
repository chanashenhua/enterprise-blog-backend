package com.company.blog.review.api;

record PermissionCheckResponse(boolean allowed, String reason) {
}