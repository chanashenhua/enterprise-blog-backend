package com.company.blog.comment.api;

import org.springframework.http.HttpHeaders;

@FunctionalInterface
public interface ArticleAccessClient {
    void requireReadable(String articleId, HttpHeaders callerHeaders);
}
