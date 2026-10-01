package com.company.blog.article.api;

import java.util.Set;

public record UpdateDraftRequest(String title, String contentJson, Set<String> tagIds, String categoryId,
                                 Long expectedRevision, boolean autosave) {
    public UpdateDraftRequest {
        tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
        categoryId = categoryId == null || categoryId.isBlank() ? null : categoryId;
    }

}
