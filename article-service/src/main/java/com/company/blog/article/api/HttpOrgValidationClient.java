package com.company.blog.article.api;

import com.company.blog.common.security.ArticlePublishScope;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class HttpOrgValidationClient implements OrgValidationClient {
    private final RestClient client;
    private final String token;

    @Autowired
    public HttpOrgValidationClient(@Value("${blog.services.org-service-url:http://org-service:8082}") String baseUrl,
                                   @Value("${blog.internal.org-token:}") String token) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.token = token;
    }

    HttpOrgValidationClient(RestClient client, String token) { this.client = client; this.token = token; }

    @Override
    public void validate(String visibilityType, Set<String> ids) {
        String error = ArticlePublishScope.validationError(visibilityType, ids);
        if (error != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        if (ArticlePublishScope.normalize(visibilityType).equals("COMPANY")) return;
        if (token.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "组织校验服务未配置，请联系管理员");
        ValidationResponse response;
        try {
            response = client.post().uri("/internal/organizations/validate-targets")
                    .header("X-Internal-Org-Token", token)
                    .body(Map.of("visibilityType", visibilityType, "targetOrgIds", ids))
                    .retrieve().body(ValidationResponse.class);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "组织校验服务暂时不可用，请稍后重试", ex);
        }
        if (response == null || response.valid() == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "组织校验响应无效，请稍后重试");
        if (!response.valid()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选部门或团队已不存在，请刷新组织列表后重新选择");
    }

    private record ValidationResponse(Boolean valid) {}
}
