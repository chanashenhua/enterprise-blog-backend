package com.company.blog.article.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class HttpOrgValidationClientTest {
    private MockRestServiceServer server;
    private HttpOrgValidationClient client;
    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl("http://org-test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpOrgValidationClient(builder.build(), "test-token");
    }
    @Test void sendsOnlyTargetsAndInternalToken() {
        server.expect(requestTo("http://org-test/internal/organizations/validate-targets"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("X-Internal-Org-Token", "test-token"))
            .andExpect(content().json("{\"visibilityType\":\"TEAM\",\"targetOrgIds\":[\"t-search\"]}"))
            .andRespond(withSuccess("{\"valid\":true}", MediaType.APPLICATION_JSON));
        client.validate("TEAM", Set.of("t-search"));
        server.verify();
    }
    @Test void invalidTargetIsBadRequest() {
        server.expect(anything()).andRespond(withSuccess("{\"valid\":false}", MediaType.APPLICATION_JSON));
        assertStatus(HttpStatus.BAD_REQUEST);
        server.verify();
    }
    @Test void unavailableServiceFailsClosed() {
        server.expect(anything()).andRespond(withServerError());
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE);
        server.verify();
    }
    @Test void missingValidationDecisionFailsClosed() {
        server.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE);
        server.verify();
    }
    @Test void companyNeedsNoOrgConnectionButCannotCarryTargets() {
        client.validate("COMPANY", Set.of());
        assertThatThrownBy(() -> client.validate("COMPANY", Set.of("t-search")))
            .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        server.verify();
    }
    @Test void missingTokenFailsClosedWithoutHttpRequest() {
        var empty = new HttpOrgValidationClient(RestClient.builder().baseUrl("http://org-test").build(), "");
        assertThatThrownBy(() -> empty.validate("TEAM", Set.of("t-search")))
            .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
    private void assertStatus(HttpStatus status) {
        assertThatThrownBy(() -> client.validate("TEAM", Set.of("t-search")))
            .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode()).isEqualTo(status));
    }
}
