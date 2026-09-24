package com.company.blog.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.article.domain.ArticleContentProjection;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArticleContentProjectionTest {
    private String markdown(String source) throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of("type", "markdown", "version", 1, "source", source));
    }

    @Test
    void rendersMarkdownAndExtractsSearchableText() throws Exception {
        var projection = ArticleContentProjection.from(markdown("# 标题\n\n**结论**与 `redis`\n\n- 列表\n\n> 边界\n\n```java\nSystem.out.println(1);\n```"));
        assertThat(projection.renderedHtml()).contains("<h1>标题</h1>", "<strong>结论</strong>",
                "<code>redis</code>", "<ul>", "<blockquote>", "class=\"language-java\"");
        assertThat(projection.plainText()).contains("标题", "结论", "redis", "System.out.println(1)").doesNotContain("**", "<h1>");
    }

    @Test
    void escapesRawHtmlAndRejectsUnsafeLinkAndImageProtocols() throws Exception {
        var projection = ArticleContentProjection.from(markdown("<script>alert(1)</script>\n\n<img src=x onerror=alert(2)>\n\n[危险](javascript:alert%281%29) ![图片](data:text/html,bad)\n\n[文档](https://example.com)"));
        assertThat(projection.renderedHtml()).doesNotContain("<script", "<img src=x", "href=\"javascript:", "src=\"data:")
                .contains("&lt;script&gt;", "href=\"https://example.com\"");
    }

    @Test
    void rejectsUnknownMarkdownVersionMalformedAndOversizedSource() throws Exception {
        assertThatThrownBy(() -> ArticleContentProjection.from("{\"type\":\"markdown\",\"version\":2,\"source\":\"text\"}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArticleContentProjection.from("{\"type\":\"markdown\",\"version\":1,\"source\":{}}"))
                .isInstanceOf(IllegalArgumentException.class);
        String large = markdown("a".repeat(100_001));
        assertThatThrownBy(() -> ArticleContentProjection.from(large)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArticleContentProjection.from("{broken")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArticleContentProjection.from("null")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArticleContentProjection.from("{\"type\":\"unknown\"}")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArticleContentProjection.from("{\"type\":\"markdown\",\"version\":4294967297,\"source\":\"text\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

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
