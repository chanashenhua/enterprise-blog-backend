package com.company.blog.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiExceptionTest {
    @Test
    void exposesErrorCodeMessageAndHttpStatus() {
        ApiException exception = new ApiException("ARTICLE_NOT_FOUND", "文章不存在", 404);

        assertThat(exception.code()).isEqualTo("ARTICLE_NOT_FOUND");
        assertThat(exception).hasMessage("文章不存在");
        assertThat(exception.httpStatus()).isEqualTo(404);
    }
}