package com.company.blog.tag.api;

import com.company.blog.tag.CatalogType;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/api/tags")
    public List<CatalogItemResponse> tags() {
        return catalogService.list(CatalogType.TAG, false);
    }

    @GetMapping("/api/categories")
    public List<CatalogItemResponse> categories() {
        return catalogService.list(CatalogType.CATEGORY, false);
    }

    @GetMapping("/internal/categories/{id}/validate")
    public CatalogValidationResponse validateCategory(@PathVariable("id") String id) {
        return catalogService.validateCategory(id);
    }
}
