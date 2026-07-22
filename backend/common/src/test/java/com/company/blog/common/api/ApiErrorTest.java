package com.company.blog.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiErrorTest {

    @Test
    void apiErrorContainsRequiredFields() {
        ApiError error = new ApiError(
            "ARTICLE_NOT_READABLE",
            "无权阅读该文章",
            "trace-1",
            Map.of("articleId", "a-1")
        );

        assertThat(error.code()).isEqualTo("ARTICLE_NOT_READABLE");
        assertThat(error.message()).isEqualTo("无权阅读该文章");
        assertThat(error.traceId()).isEqualTo("trace-1");
        assertThat(error.details()).containsEntry("articleId", "a-1");
    }
}
