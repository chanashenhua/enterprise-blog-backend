package com.company.blog.stats.api;

import org.springframework.http.HttpHeaders;

record CallerIdentity(String userId) {
    static CallerIdentity from(HttpHeaders headers) {
        return new CallerIdentity(headers.getFirst("X-User-Id"));
    }
}
