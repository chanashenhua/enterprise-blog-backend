package com.company.blog.tag.api;

import com.company.blog.tag.CatalogItem;

public record CatalogItemResponse(String id, String name, boolean active) {
    static CatalogItemResponse from(CatalogItem item) {
        return new CatalogItemResponse(item.id(), item.name(), item.active());
    }
}
