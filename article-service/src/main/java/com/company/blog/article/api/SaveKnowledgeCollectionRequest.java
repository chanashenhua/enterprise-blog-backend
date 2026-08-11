package com.company.blog.article.api;

import java.util.List;

public record SaveKnowledgeCollectionRequest(
        String title,
        String description,
        List<String> articleIds
) {
}
