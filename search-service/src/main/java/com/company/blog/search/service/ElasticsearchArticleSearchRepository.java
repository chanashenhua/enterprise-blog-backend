package com.company.blog.search.service;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.api.SearchArticleRequest;
import com.company.blog.search.api.SearchArticleResponse;
import com.company.blog.search.index.ArticleSearchDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

@Repository
public class ElasticsearchArticleSearchRepository implements ArticleSearchRepository {
    private static final String INDEX_NAME = "articles-v1";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PermissionCheckClient permissionCheckClient;
    private final SearchVisibilityFilter visibilityFilter = new SearchVisibilityFilter();

    public ElasticsearchArticleSearchRepository(
            ObjectMapper objectMapper,
            PermissionCheckClient permissionCheckClient,
            @Value("${blog.elasticsearch.base-url:http://elasticsearch:9200}") String baseUrl
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.permissionCheckClient = permissionCheckClient;
    }

    @Override
    public void index(ArticleSearchDocument document) {
        restClient.put()
                .uri("/" + INDEX_NAME + "/_doc/{articleId}", document.articleId())
                .body(indexJson(document))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void delete(String articleId) {
        restClient.delete()
                .uri("/" + INDEX_NAME + "/_doc/{articleId}", articleId)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public SearchArticleResponse search(UserContext user, SearchArticleRequest request) {
        String response = restClient.post()
                .uri("/" + INDEX_NAME + "/_search")
                .body(searchJson(user, request))
                .retrieve()
                .body(String.class);
        return response == null ? new SearchArticleResponse(List.of(), 0, request.resolvedPage(), request.resolvedSize())
                : parseSearchResponse(user, request, response);
    }

    private String indexJson(ArticleSearchDocument document) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("article_id", document.articleId());
        node.put("title", document.title());
        node.put("summary", document.summary());
        node.put("plain_text", document.plainText());
        node.set("tags", objectMapper.valueToTree(document.tags()));
        node.put("author_id", document.authorId());
        node.put("author_name", document.authorName());
        node.put("visibility_type", document.visibilityType().toLowerCase());
        node.set("target_org_ids", objectMapper.valueToTree(document.targetOrgIds()));
        node.put("status", document.status());
        node.put("published_at", document.publishedAt().toString());
        node.put("updated_at", document.updatedAt().toString());
        return node.toString();
    }

    private String searchJson(UserContext user, SearchArticleRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("from", request.resolvedPage() * request.resolvedSize());
        root.put("size", request.resolvedSize());
        root.putArray("sort").addObject().put("published_at", "desc");
        ObjectNode bool = root.putObject("query").putObject("bool");
        ArrayNode filters = bool.putArray("filter");
        filters.addObject().putObject("term").put("status", "PUBLISHED");
        ObjectNode visibilityBool = filters.addObject().putObject("bool");
        ArrayNode visibility = visibilityBool.putArray("should");
        visibility.addObject().putObject("term").put("visibility_type", "company");
        addScopedVisibility(visibility, "department", user.departmentIds());
        addScopedVisibility(visibility, "team", user.teamIds());
        visibilityBool.put("minimum_should_match", 1);
        if (request.query() != null && !request.query().isBlank()) {
            ObjectNode multiMatch = bool.putArray("must").addObject().putObject("multi_match");
            multiMatch.put("query", request.query());
            multiMatch.putArray("fields").add("title^3").add("summary^2").add("plain_text");
        }
        return root.toString();
    }

    private static void addScopedVisibility(ArrayNode visibility, String type, Set<String> targetIds) {
        if (targetIds.isEmpty()) {
            return;
        }
        ObjectNode clause = visibility.addObject().putObject("bool");
        ArrayNode must = clause.putArray("must");
        must.addObject().putObject("term").put("visibility_type", type);
        ArrayNode targets = must.addObject().putObject("terms").putArray("target_org_ids");
        targetIds.forEach(targets::add);
    }

    private SearchArticleResponse parseSearchResponse(UserContext user, SearchArticleRequest request, String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            long total = root.path("hits").path("total").path("value").asLong();
            List<SearchArticleResponse.Article> items = new ArrayList<>();
            for (JsonNode hit : root.path("hits").path("hits")) {
                ArticleSearchDocument document = documentFrom(hit.path("_source"));
                if (visibilityFilter.isVisible(user, document) && permissionCheckClient.canRead(user, document)) {
                    items.add(new SearchArticleResponse.Article(
                            document.articleId(), document.title(), document.summary(), document.tags(),
                            document.authorId(), document.authorName(), document.publishedAt(), document.updatedAt()
                    ));
                }
            }
            return new SearchArticleResponse(items, total, request.resolvedPage(), request.resolvedSize());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse Elasticsearch response", ex);
        }
    }

    private static ArticleSearchDocument documentFrom(JsonNode source) {
        return new ArticleSearchDocument(
                source.path("article_id").asText(), source.path("title").asText(), source.path("summary").asText(),
                source.path("plain_text").asText(), stringSet(source.path("tags")), source.path("author_id").asText(),
                source.path("author_name").asText(), source.path("visibility_type").asText(),
                stringSet(source.path("target_org_ids")), source.path("status").asText(),
                Instant.parse(source.path("published_at").asText()), Instant.parse(source.path("updated_at").asText())
        );
    }

    private static Set<String> stringSet(JsonNode node) {
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        node.forEach(value -> values.add(value.asText()));
        return Set.copyOf(values);
    }
}
