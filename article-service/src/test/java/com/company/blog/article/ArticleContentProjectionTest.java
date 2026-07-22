package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.article.domain.ArticleContentProjection;
import org.junit.jupiter.api.Test;

class ArticleContentProjectionTest {
    @Test
    void extractsPlainTextFromEditorJson() {
        String title = "Redis \u7f13\u5b58\u7b56\u7565";
        String contentJson = """
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"%s"}]}]}
                """.formatted(title);

        ArticleContentProjection projection = ArticleContentProjection.from(contentJson);

        assertThat(projection.plainText()).isEqualTo(title);
        assertThat(projection.renderedHtml()).contains("<p>" + title + "</p>");
    }
}