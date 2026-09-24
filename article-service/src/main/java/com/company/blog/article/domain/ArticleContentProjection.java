package com.company.blog.article.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.DefaultUrlSanitizer;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;

/**
 * 从编辑器 JSON 生成的安全展示与检索投影。
 *
 * <p>HTML 由受控节点重新渲染并转义文本，不能直接把客户端传入的 HTML 返回给页面；
 * 纯文本则供摘要和 Elasticsearch 全文检索使用。</p>
 */
public record ArticleContentProjection(String renderedHtml, String plainText) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Parser MARKDOWN = Parser.builder().build();
    private static final HtmlRenderer HTML = HtmlRenderer.builder()
            .escapeHtml(true).sanitizeUrls(true)
            .urlSanitizer(new DefaultUrlSanitizer(List.of("http", "https", "mailto")))
            .build();
    private static final TextContentRenderer TEXT = TextContentRenderer.builder().build();
    public static final int MAX_SOURCE_LENGTH = 100_000;

    public static ArticleContentProjection from(String contentJson) {
        try {
            if (contentJson == null || contentJson.length() > 1_000_000) {
                throw new IllegalArgumentException("Article content is missing or too large");
            }
            JsonNode root = OBJECT_MAPPER.readTree(contentJson);
            if (root != null && "markdown".equals(root.path("type").asText())) {
                if (!root.path("version").isInt() || root.path("version").asInt() != 1
                        || !root.path("source").isTextual()
                        || root.path("source").asText().length() > MAX_SOURCE_LENGTH) {
                    throw new IllegalArgumentException("Unsupported Markdown version or source");
                }
                var document = MARKDOWN.parse(root.path("source").asText());
                return new ArticleContentProjection(HTML.render(document).trim(), TEXT.render(document).trim());
            }
            if (root == null || !"doc".equals(root.path("type").asText()) || !root.path("content").isArray()) {
                throw new IllegalArgumentException("Unsupported article document format");
            }
            ProjectionBuilder builder = new ProjectionBuilder();
            renderDocument(root, builder);
            return new ArticleContentProjection(builder.html().trim(), builder.plainText().trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid article content JSON", ex);
        }
    }

    private static void renderDocument(JsonNode node, ProjectionBuilder builder) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        JsonNode content = node.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode child : content) {
                renderNode(child, builder);
            }
        }
    }

    private static void renderNode(JsonNode node, ProjectionBuilder builder) {
        String type = node.path("type").asText("");
        if ("paragraph".equals(type)) {
            String text = collectText(node);
            if (!text.isBlank()) {
                builder.addParagraph(text);
            }
            return;
        }
        if ("text".equals(type)) {
            builder.addInlineText(node.path("text").asText(""));
            return;
        }
        renderDocument(node, builder);
    }

    private static String collectText(JsonNode node) {
        List<String> fragments = new ArrayList<>();
        collectTextFragments(node, fragments);
        return String.join("", fragments);
    }

    private static void collectTextFragments(JsonNode node, List<String> fragments) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        if ("text".equals(node.path("type").asText(""))) {
            fragments.add(node.path("text").asText(""));
        }
        JsonNode content = node.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode child : content) {
                collectTextFragments(child, fragments);
            }
        }
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static final class ProjectionBuilder {
        private final StringBuilder html = new StringBuilder();
        private final StringBuilder plainText = new StringBuilder();

        void addParagraph(String text) {
            html.append("<p>").append(escapeHtml(text)).append("</p>");
            if (!plainText.isEmpty()) {
                plainText.append(System.lineSeparator());
            }
            plainText.append(text);
        }

        void addInlineText(String text) {
            html.append(escapeHtml(text));
            plainText.append(text);
        }

        String html() {
            return html.toString();
        }

        String plainText() {
            return plainText.toString();
        }
    }
}
