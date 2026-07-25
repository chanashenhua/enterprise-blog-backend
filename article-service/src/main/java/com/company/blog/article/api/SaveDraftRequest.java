package com.company.blog.article.api;

import java.util.Set;

public record SaveDraftRequest(String title, String contentJson, Set<String> tagIds, String categoryId) {
    public SaveDraftRequest {
        tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
        categoryId = categoryId == null || categoryId.isBlank() ? null : categoryId;
    }

    public SaveDraftRequest(String title, String contentJson, Set<String> tagIds) {
        this(title, contentJson, tagIds, null);
    }
}
