package com.company.blog.tag.api;

import com.company.blog.tag.CatalogType;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
/** 管理后台标签和分类目录接口。 */
public class AdminTagController {
    private final CatalogService catalogService;

    public AdminTagController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/api/admin/tags")
    public List<CatalogItemResponse> listTags(@RequestHeader HttpHeaders headers) {
        requireAdmin(headers);
        return catalogService.list(CatalogType.TAG, true);
    }

    @PostMapping("/api/admin/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemResponse createTag(
            @RequestHeader HttpHeaders headers,
            @RequestBody CatalogItemRequest request
    ) {
        requireAdmin(headers);
        return catalogService.create(CatalogType.TAG, request, actorId(headers), roles(headers));
    }

    @PutMapping("/api/admin/tags/{id}")
    public CatalogItemResponse updateTag(
            @RequestHeader HttpHeaders headers,
            @PathVariable("id") String id,
            @RequestBody CatalogItemRequest request
    ) {
        requireAdmin(headers);
        return catalogService.update(CatalogType.TAG, id, request, actorId(headers), roles(headers));
    }

    @DeleteMapping("/api/admin/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateTag(@RequestHeader HttpHeaders headers, @PathVariable("id") String id) {
        requireAdmin(headers);
        catalogService.deactivate(CatalogType.TAG, id, actorId(headers), roles(headers));
    }

    @GetMapping("/api/admin/categories")
    public List<CatalogItemResponse> listCategories(@RequestHeader HttpHeaders headers) {
        requireAdmin(headers);
        return catalogService.list(CatalogType.CATEGORY, true);
    }

    @PostMapping("/api/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemResponse createCategory(
            @RequestHeader HttpHeaders headers,
            @RequestBody CatalogItemRequest request
    ) {
        requireAdmin(headers);
        return catalogService.create(CatalogType.CATEGORY, request, actorId(headers), roles(headers));
    }

    @PutMapping("/api/admin/categories/{id}")
    public CatalogItemResponse updateCategory(
            @RequestHeader HttpHeaders headers,
            @PathVariable("id") String id,
            @RequestBody CatalogItemRequest request
    ) {
        requireAdmin(headers);
        return catalogService.update(CatalogType.CATEGORY, id, request, actorId(headers), roles(headers));
    }

    @DeleteMapping("/api/admin/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateCategory(@RequestHeader HttpHeaders headers, @PathVariable("id") String id) {
        requireAdmin(headers);
        catalogService.deactivate(CatalogType.CATEGORY, id, actorId(headers), roles(headers));
    }

    private static void requireAdmin(HttpHeaders headers) {
        String roles = headers.getFirst("X-User-Roles");
        if (roles == null || java.util.Arrays.stream(roles.split(",")).map(String::trim).noneMatch("ADMIN"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role is required");
        }
    }

    private static String actorId(HttpHeaders headers) {
        String actorId = headers.getFirst("X-User-Id");
        return actorId == null || actorId.isBlank() ? "unknown" : actorId.trim();
    }

    private static List<String> roles(HttpHeaders headers) {
        String roles = headers.getFirst("X-User-Roles");
        return roles == null || roles.isBlank()
                ? List.of()
                : java.util.Arrays.stream(roles.split(",")).map(String::trim).filter(role -> !role.isBlank()).toList();
    }
}
