package com.company.blog.tag.api;

import com.company.blog.tag.CatalogItem;
import com.company.blog.tag.CatalogType;
import java.util.List;
import java.util.Optional;

public interface CatalogRepository {
    List<CatalogItem> findAll(CatalogType type, boolean includeInactive);

    Optional<CatalogItem> findById(CatalogType type, String id);

    CatalogItem create(CatalogType type, String id, String name);

    Optional<CatalogItem> update(CatalogType type, String id, String name);

    Optional<CatalogItem> deactivate(CatalogType type, String id);
}
