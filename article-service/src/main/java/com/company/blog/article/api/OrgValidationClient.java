package com.company.blog.article.api;

import java.util.Set;

@FunctionalInterface
public interface OrgValidationClient {
    void validate(String visibilityType, Set<String> targetOrgIds);
}
