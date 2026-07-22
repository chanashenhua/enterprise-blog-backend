package com.company.blog.search.api;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.service.SearchService;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/search/articles")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchArticleResponse search(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        UserContext user = userContext(headers);
        return searchService.search(user, new SearchArticleRequest(query, page, size));
    }

    private static UserContext userContext(HttpHeaders headers) {
        String userId = headers.getFirst("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id is required");
        }
        return new UserContext(
                userId,
                headerSet(headers, "X-User-Roles"),
                headerSet(headers, "X-Department-Ids"),
                headerSet(headers, "X-Team-Ids")
        );
    }

    private static Set<String> headerSet(HttpHeaders headers, String name) {
        String raw = headers.getFirst(name);
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
