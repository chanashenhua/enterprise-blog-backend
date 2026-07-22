package com.company.blog.search.api;

public record SearchArticleRequest(String query, Integer page, Integer size) {
    public int resolvedPage() {
        return page == null ? 0 : Math.max(page, 0);
    }

    public int resolvedSize() {
        return size == null ? 20 : Math.min(Math.max(size, 1), 100);
    }
}
