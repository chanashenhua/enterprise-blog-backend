package com.company.blog.stats.api;

import org.springframework.http.HttpHeaders;

public interface ArticleAccessClient {
    void requireReadable(String articleId, HttpHeaders callerHeaders);
}
