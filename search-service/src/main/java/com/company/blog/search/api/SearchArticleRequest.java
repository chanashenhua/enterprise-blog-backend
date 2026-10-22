package com.company.blog.search.api;

public record SearchArticleRequest(String query, String categoryId, String tagId, Integer page, Integer size, String sort) {
    public SearchArticleRequest {
        query = query == null ? "" : query.trim();
        sort = sort == null || sort.isBlank() ? "relevance" : sort;
        if (query.length() > 200 || (page != null && (page < 0 || page > 99))
                || (size != null && (size < 1 || size > 50))
                || !java.util.Set.of("relevance", "latest").contains(sort)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "搜索参数无效：页码 0–99，每页 1–50 条，关键词最多 200 字");
        }
    }

    public SearchArticleRequest(String query, String categoryId, String tagId, Integer page, Integer size) {
        this(query, categoryId, tagId, page, size, "relevance");
    }
    public SearchArticleRequest(String query, Integer page, Integer size) {
        this(query, null, null, page, size, "relevance");
    }

    public int resolvedPage() {
        return page == null ? 0 : Math.max(page, 0);
    }

    public int resolvedSize() {
        return size == null ? 20 : size;
    }
}
