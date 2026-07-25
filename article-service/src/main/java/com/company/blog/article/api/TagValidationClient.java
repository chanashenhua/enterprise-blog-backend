package com.company.blog.article.api;

import java.util.Set;

public interface TagValidationClient {
    void validate(Set<String> tagIds);

    default void validateCategory(String categoryId) {
    }
}
