package com.company.blog.article.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * 从编辑器 JSON 生成的安全展示与检索投影。
 *
 * <p>HTML 由受控节点重新渲染并转义文本，不能直接把客户端传入的 HTML 返回给页面；
 * 纯文本则供摘要和 Elasticsearch 全文检索使用。</p>
 */
public record ArticleContentProjection(String renderedHtml, String plainText) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static ArticleContentProjection from(String contentJson) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(contentJson);
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
