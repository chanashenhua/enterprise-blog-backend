package com.company.blog.search.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class SearchExceptionHandler {
    @ExceptionHandler({RestClientException.class, IllegalStateException.class})
    ResponseEntity<Map<String, String>> unavailable(Exception exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "搜索或权限校验服务暂时不可用，请稍后重试"));
    }
}
