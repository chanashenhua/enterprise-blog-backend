package com.company.blog.stats.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * 通过文章详情接口复用文章服务的可见范围与权限判断。
 *
 * <p>统计服务只保存互动事实，不能因为拿到了文章标识就绕过内容域授权。</p>
 */
@Component
public class HttpArticleAccessClient implements ArticleAccessClient {
    private static final List<String> CALLER_HEADERS = List.of(
            "X-User-Id",
            "X-User-Roles",
            "X-Department-Ids",
            "X-Team-Ids"
    );

    private final RestClient restClient;

    public HttpArticleAccessClient(
            @Value("${blog.services.article-service-url:http://article-service:8084}") String baseUrl
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public void requireReadable(String articleId, HttpHeaders callerHeaders) {
        try {
            restClient.get()
                    .uri("/api/articles/{articleId}", articleId)
                    .headers(target -> CALLER_HEADERS.forEach(name -> {
                        String value = callerHeaders.getFirst(name);
                        if (value != null) {
                            target.set(name, value);
                        }
                    }))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            throw new ResponseStatusException(ex.getStatusCode(), "Article is not readable", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Article service is unavailable", ex);
        }
    }
}
