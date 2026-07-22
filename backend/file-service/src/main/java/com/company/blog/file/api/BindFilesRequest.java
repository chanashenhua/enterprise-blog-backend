package com.company.blog.file.api;

import java.util.Set;

public record BindFilesRequest(String ownerId, String resourceType, String resourceId, Set<String> fileIds) {
    public BindFilesRequest {
        fileIds = fileIds == null ? Set.of() : Set.copyOf(fileIds);
    }
}
