package com.company.blog.search.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.company.blog.common.security.UserContext;
import com.company.blog.search.api.SearchArticleRequest;
import com.company.blog.search.index.ArticleSearchDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class ElasticsearchRepositoryTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UserContext USER = new UserContext("u-reader", Set.of("READER"), Set.of("d-platform"), Set.of("t-search"));
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private ElasticsearchArticleSearchRepository repository(PermissionCheckClient permissions) {
        return new ElasticsearchArticleSearchRepository(MAPPER, permissions, "http://es.test", builder);
    }

    private static ObjectNode hit(String id) {
        ObjectNode hit = MAPPER.createObjectNode();
        ObjectNode source = hit.putObject("_source");
        source.put("article_id", id).put("title", "Redis <img onerror=alert(1)>").put("summary", "缓存摘要")
                .put("plain_text", "Redis 缓存正文").put("category_id", "engineering")
                .put("author_id", "u-author").put("author_name", "作者").put("visibility_type", "company")
                .put("status", "PUBLISHED").put("published_at", "2026-10-01T00:00:00Z")
                .put("updated_at", "2026-10-01T00:00:00Z").put("revision", 3);
        source.putArray("tags").add("redis");
        source.putArray("target_org_ids");
        hit.putObject("highlight").putArray("title").add("\uE000Redis\uE001 <img onerror=alert(1)>");
        return hit;
    }

    private static String hits(int total, List<ObjectNode> nodes) throws Exception {
        ObjectNode response = MAPPER.createObjectNode();
        ObjectNode hits = response.putObject("hits");
        hits.putObject("total").put("value", total);
        ArrayNode array = hits.putArray("hits");
        nodes.forEach(array::add);
        return MAPPER.writeValueAsString(response);
    }

    @Test
    void buildsFilteredStableQueryAndReturnsPlainTextHighlights() throws Exception {
        server.expect(requestTo("http://es.test/articles-v1/_search")).andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    var query = MAPPER.readTree(((MockClientHttpRequest) request).getBodyAsString());
                    assertThat(query.path("track_total_hits").asBoolean()).isTrue();
                    assertThat(query.toString()).contains("\"category_id\":\"engineering\"", "\"tags\":\"redis\"",
                            "\"visibility_type\":\"company\"", "\"target_org_ids\":[\"t-search\"]");
                    assertThat(query.path("sort").get(0).has("_score")).isTrue();
                    assertThat(query.path("highlight").path("pre_tags").get(0).asText()).isEqualTo("\uE000");
                    assertThat(query.path("query").path("bool").path("must").get(0).path("multi_match").path("type").asText()).isEqualTo("cross_fields");
                    assertThat(query.path("highlight").path("highlight_query").path("multi_match").path("operator").asText()).isEqualTo("or");
                }).andRespond(withSuccess(hits(1, List.of(hit("a-1"))), MediaType.APPLICATION_JSON));
        var response = repository((user, document) -> true).search(USER,
                new SearchArticleRequest("Redis", "engineering", "redis", 0, 10, "relevance"));
        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).titleHighlights()).anySatisfy(part -> {
            assertThat(part.text()).contains("<img");
            assertThat(part.matched()).isFalse();
        });
        server.verify();
    }

    @Test
    void filtersRevokedHitsBeforePaginationAndDoesNotLeakRawCount() throws Exception {
        server.expect(requestTo("http://es.test/articles-v1/_search"))
                .andRespond(withSuccess(hits(3, List.of(hit("revoked"), hit("allowed-1"), hit("allowed-2"))), MediaType.APPLICATION_JSON));
        var response = repository((user, document) -> !document.articleId().equals("revoked"))
                .search(USER, new SearchArticleRequest("", null, null, 0, 1, "latest"));
        assertThat(response.items()).extracting(item -> item.articleId()).containsExactly("allowed-1");
        assertThat(response.total()).isEqualTo(2L);
        assertThat(response.hasMore()).isTrue();
        server.verify();
    }

    @Test
    void retriesAreVersionedAndDeletionRetainsNoArticleContent() {
        var repo = repository((user, document) -> true);
        server.expect(requestTo("http://es.test/articles-v1/_doc/a-1?version=7&version_type=external_gte"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"article_id\":\"a-1\",\"revision\":7,\"status\":\"DELETED\"}", true))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://es.test/articles-v1/_doc/a-1?version=6&version_type=external_gte"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        repo.delete("a-1", 7);
        repo.delete("a-1", 6);
        server.verify();
    }

    @Test
    void unknownTotalIsNotReportedAsAnExactAccessibleCount() throws Exception {
        var nodes = java.util.stream.IntStream.range(0, 100).mapToObj(i -> hit("a-" + i)).toList();
        server.expect(requestTo("http://es.test/articles-v1/_search"))
                .andRespond(withSuccess(hits(200, nodes), MediaType.APPLICATION_JSON));
        var result = repository((user, document) -> true).search(USER, new SearchArticleRequest("Redis", 0, 20));
        assertThat(result.total()).isNull();
        assertThat(result.hasMore()).isTrue();
        assertThat(result.items()).hasSize(20);
    }

    @Test
    void rejectsUnboundedRequestsAndUnsupportedSort() {
        assertThatThrownBy(() -> new SearchArticleRequest("x", 100, 10)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> new SearchArticleRequest("x", 0, 51)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> new SearchArticleRequest("x", null, null, 0, 10, "arbitrary")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void permissionsUnavailableDoesNotReturnUnverifiedContent() throws Exception {
        server.expect(requestTo("http://es.test/articles-v1/_search"))
                .andRespond(withSuccess(hits(1, List.of(hit("a-1"))), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> repository((user, document) -> { throw new IllegalStateException("offline"); })
                .search(USER, new SearchArticleRequest("", 0, 10))).isInstanceOf(IllegalStateException.class);
    }
}
