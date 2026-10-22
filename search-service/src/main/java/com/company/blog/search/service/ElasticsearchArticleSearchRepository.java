package com.company.blog.search.service;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.api.SearchArticleRequest;
import com.company.blog.search.api.SearchArticleResponse;
import com.company.blog.search.api.SearchArticleResponse.HighlightPart;
import com.company.blog.search.index.ArticleSearchDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

@Repository
public class ElasticsearchArticleSearchRepository implements ArticleSearchRepository {
    static final String INDEX_NAME = "articles-v1";
    private static final int BATCH_SIZE = 100;
    private static final int MAX_CANDIDATES = 10_000;
    private final RestClient client;
    private final ObjectMapper mapper;
    private final PermissionCheckClient permissions;
    private final SearchVisibilityFilter visibility = new SearchVisibilityFilter();

    public ElasticsearchArticleSearchRepository(ObjectMapper mapper, PermissionCheckClient permissions,
            @Value("${blog.elasticsearch.base-url:http://elasticsearch:9200}") String baseUrl,
            RestClient.Builder builder) {
        this.client = builder.baseUrl(baseUrl).build();
        this.mapper = mapper;
        this.permissions = permissions;
    }

    @Override
    public void index(ArticleSearchDocument document) {
        if (document.revision() < 1 || !"PUBLISHED".equals(document.status())
                || document.articleId() == null || document.title() == null || document.visibilityType() == null
                || document.publishedAt() == null || document.updatedAt() == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid search projection");
        }
        ObjectNode body = mapper.createObjectNode();
        body.put("article_id", document.articleId());
        body.put("title", document.title());
        body.put("summary", document.summary());
        body.put("plain_text", document.plainText());
        body.set("tags", mapper.valueToTree(document.tags()));
        if (document.categoryId() != null) body.put("category_id", document.categoryId());
        body.put("author_id", document.authorId());
        body.put("author_name", document.authorName());
        body.put("visibility_type", document.visibilityType().toLowerCase(java.util.Locale.ROOT));
        body.set("target_org_ids", mapper.valueToTree(document.targetOrgIds()));
        body.put("status", document.status());
        body.put("published_at", document.publishedAt().toString());
        body.put("updated_at", document.updatedAt().toString());
        body.put("revision", document.revision());
        write(document.articleId(), document.revision(), body);
    }

    @Override
    public void delete(String articleId, long revision) {
        if (revision < 1) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "revision is required");
        // Keep only a versioned tombstone: delayed retries cannot resurrect removed text.
        write(articleId, revision, mapper.valueToTree(Map.of(
                "article_id", articleId, "revision", revision, "status", "DELETED")));
    }

    private void write(String id, long revision, JsonNode body) {
        client.put().uri(builder -> builder.path("/" + INDEX_NAME + "/_doc/{id}")
                .queryParam("version", revision).queryParam("version_type", "external_gte").build(id))
                .body(body).retrieve()
                .onStatus(status -> status.value() == 409, (request, response) -> { /* A newer revision already won. */ })
                .toBodilessEntity();
    }

    @Override
    public SearchArticleResponse search(UserContext user, SearchArticleRequest request) {
        int start = request.resolvedPage() * request.resolvedSize();
        int needed = start + request.resolvedSize() + 1;
        List<SearchArticleResponse.Article> authorized = new ArrayList<>();
        int scanned = 0;
        boolean exhausted = false;
        while (scanned < MAX_CANDIDATES && authorized.size() < needed) {
            JsonNode response = client.post().uri("/" + INDEX_NAME + "/_search")
                    .body(searchJson(user, request, scanned)).retrieve().body(JsonNode.class);
            if (response == null || !response.path("hits").path("hits").isArray()) {
                throw new IllegalStateException("Invalid Elasticsearch response");
            }
            JsonNode hits = response.path("hits").path("hits");
            List<ArticleSearchDocument> candidates = new ArrayList<>();
            List<JsonNode> matchingHits = new ArrayList<>();
            for (JsonNode hit : hits) {
                ArticleSearchDocument document = documentFrom(hit.path("_source"));
                if (visibility.isVisible(user, document)) {
                    candidates.add(document);
                    matchingHits.add(hit);
                }
            }
            // The article database, including its CURRENT revision and visibility, is authoritative.
            Set<String> allowedIds = candidates.isEmpty() ? Set.of() : permissions.readableIds(user, candidates);
            for (int i = 0; i < candidates.size(); i++) {
                ArticleSearchDocument document = candidates.get(i);
                if (!allowedIds.contains(document.articleId())) continue;
                JsonNode highlight = matchingHits.get(i).path("highlight");
                authorized.add(new SearchArticleResponse.Article(document.articleId(), document.title(), document.summary(),
                        document.tags(), document.categoryId(), document.authorId(), document.authorName(),
                        document.publishedAt(), document.updatedAt(),
                        parts(fragment(highlight, "title", document.title())),
                        parts(fragment(highlight, "plain_text", document.summary()))));
            }
            scanned += hits.size();
            if (hits.size() < BATCH_SIZE || scanned >= response.path("hits").path("total").path("value").asLong()) {
                exhausted = true;
                break;
            }
        }
        int end = Math.min(start + request.resolvedSize(), authorized.size());
        List<SearchArticleResponse.Article> page = start >= authorized.size() ? List.of() : authorized.subList(start, end);
        boolean hasMore = authorized.size() > end;
        // Never expose the raw ES count: it can include revoked or stale articles.
        return new SearchArticleResponse(page, exhausted ? (long) authorized.size() : null,
                request.resolvedPage(), request.resolvedSize(), hasMore, !exhausted && scanned >= MAX_CANDIDATES);
    }

    private ObjectNode searchJson(UserContext user, SearchArticleRequest request, int offset) {
        ObjectNode root = mapper.createObjectNode();
        root.put("from", offset).put("size", BATCH_SIZE).put("track_total_hits", true);
        ArrayNode sort = root.putArray("sort");
        if ("relevance".equals(request.sort()) && !request.query().isBlank()) sort.addObject().put("_score", "desc");
        sort.addObject().put("published_at", "desc");
        sort.addObject().put("article_id", "asc");
        ObjectNode bool = root.putObject("query").putObject("bool");
        ArrayNode filters = bool.putArray("filter");
        filters.addObject().putObject("term").put("status", "PUBLISHED");
        if (request.categoryId() != null && !request.categoryId().isBlank())
            filters.addObject().putObject("term").put("category_id", request.categoryId().trim());
        if (request.tagId() != null && !request.tagId().isBlank())
            filters.addObject().putObject("term").put("tags", request.tagId().trim());
        ObjectNode scope = filters.addObject().putObject("bool");
        ArrayNode scopes = scope.putArray("should");
        scopes.addObject().putObject("term").put("visibility_type", "company");
        addScope(scopes, "department", user.departmentIds());
        addScope(scopes, "team", user.teamIds());
        scope.put("minimum_should_match", 1);
        if (!request.query().isBlank()) {
            ObjectNode match = bool.putArray("must").addObject().putObject("multi_match");
            match.put("query", request.query()).put("type", "cross_fields").put("operator", "and");
            match.putArray("fields").add("title^3").add("summary^2").add("plain_text");
            ObjectNode highlight = root.putObject("highlight");
            highlight.putArray("pre_tags").add("\uE000");
            highlight.putArray("post_tags").add("\uE001");
            // Highlight any matched term in each field, even when other terms matched the title.
            ObjectNode highlightQuery = highlight.putObject("highlight_query").putObject("multi_match");
            highlightQuery.put("query", request.query()).put("operator", "or");
            highlightQuery.putArray("fields").add("title").add("plain_text");
            ObjectNode fields = highlight.putObject("fields");
            fields.putObject("title").put("number_of_fragments", 0);
            fields.putObject("plain_text").put("number_of_fragments", 1).put("fragment_size", 180);
        }
        return root;
    }

    private static void addScope(ArrayNode scopes, String type, Set<String> ids) {
        if (ids.isEmpty()) return;
        ArrayNode must = scopes.addObject().putObject("bool").putArray("must");
        must.addObject().putObject("term").put("visibility_type", type);
        ArrayNode targets = must.addObject().putObject("terms").putArray("target_org_ids");
        ids.forEach(targets::add);
    }

    private static String fragment(JsonNode highlights, String field, String fallback) {
        JsonNode values = highlights.path(field);
        return values.isArray() && !values.isEmpty() ? values.get(0).asText() : (fallback == null ? "" : fallback);
    }

    static List<HighlightPart> parts(String fragment) {
        List<HighlightPart> result = new ArrayList<>();
        boolean matched = false;
        StringBuilder text = new StringBuilder();
        for (char character : fragment.toCharArray()) {
            if (character == '\uE000' || character == '\uE001') {
                if (!text.isEmpty()) result.add(new HighlightPart(text.toString(), matched));
                text.setLength(0);
                matched = character == '\uE000';
            } else text.append(character);
        }
        if (!text.isEmpty()) result.add(new HighlightPart(text.toString(), matched));
        return result;
    }

    private static ArticleSearchDocument documentFrom(JsonNode source) {
        return new ArticleSearchDocument(source.path("article_id").asText(), source.path("title").asText(),
                source.path("summary").asText(), source.path("plain_text").asText(), strings(source.path("tags")),
                source.hasNonNull("category_id") ? source.path("category_id").asText() : null,
                source.path("author_id").asText(), source.path("author_name").asText(),
                source.path("visibility_type").asText(), strings(source.path("target_org_ids")), source.path("status").asText(),
                Instant.parse(source.path("published_at").asText()), Instant.parse(source.path("updated_at").asText()),
                source.path("revision").asLong());
    }

    private static Set<String> strings(JsonNode node) {
        Set<String> result = new java.util.LinkedHashSet<>();
        node.forEach(value -> result.add(value.asText()));
        return result;
    }
}
