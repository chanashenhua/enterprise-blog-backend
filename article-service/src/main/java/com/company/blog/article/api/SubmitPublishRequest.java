package com.company.blog.article.api;

import java.util.Set;

public record SubmitPublishRequest(String visibilityType, Set<String> targetOrgIds, boolean reviewRequired,
                                   Long expectedRevision) {
    public SubmitPublishRequest {
        targetOrgIds = targetOrgIds == null ? Set.of() : Set.copyOf(targetOrgIds);
    }

    public SubmitPublishRequest(String visibilityType, Set<String> targetOrgIds, boolean reviewRequired) {
        this(visibilityType, targetOrgIds, reviewRequired, null);
    }
}
