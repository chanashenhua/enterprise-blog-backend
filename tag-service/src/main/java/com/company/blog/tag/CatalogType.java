package com.company.blog.tag;

public enum CatalogType {
    TAG("tag"),
    CATEGORY("category");

    private final String tableName;

    CatalogType(String tableName) {
        this.tableName = tableName;
    }

    public String tableName() {
        return tableName;
    }
}
