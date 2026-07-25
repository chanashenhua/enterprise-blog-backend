package com.company.blog.article.api;

import java.util.Set;

public record UpdateDraftRequest(String title, String contentJson, Set<String> tagIds) {
    public UpdateDraftRequest {
        tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
    }
}
