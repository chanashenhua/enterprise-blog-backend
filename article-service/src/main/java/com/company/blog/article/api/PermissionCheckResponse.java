package com.company.blog.article.api;

record PermissionCheckResponse(boolean allowed, String reason) {
}