package com.company.blog.article.api;

import java.util.Set;

public record SaveDraftRequest(String title, String contentJson, Set<String> tagIds) {
    public SaveDraftRequest {
        tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
    }
}