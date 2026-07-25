package com.company.blog.tag.api;

import com.company.blog.tag.CatalogItem;
import com.company.blog.tag.CatalogType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogService {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}");

    private final CatalogRepository repository;

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> list(CatalogType type, boolean includeInactive) {
        return repository.findAll(type, includeInactive).stream()
                .map(CatalogItemResponse::from)
                .toList();
    }

    @Transactional
    public CatalogItemResponse create(CatalogType type, CatalogItemRequest request) {
        String id = requireId(request == null ? null : request.id());
        String name = requireName(request == null ? null : request.name());
        try {
            return CatalogItemResponse.from(repository.create(type, id, name));
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Catalog id or name already exists", ex);
        }
    }

    @Transactional
    public CatalogItemResponse update(CatalogType type, String id, CatalogItemRequest request) {
        String validId = requireId(id);
        String name = requireName(request == null ? null : request.name());
        try {
            CatalogItem updated = repository.update(type, validId, name)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalog item not found"));
            return CatalogItemResponse.from(updated);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Catalog name already exists", ex);
        }
    }

    @Transactional
    public void deactivate(CatalogType type, String id) {
        repository.deactivate(type, requireId(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalog item not found"));
    }

    @Transactional(readOnly = true)
    public TagValidationResponse validateTags(List<String> ids) {
        LinkedHashSet<String> requested = new LinkedHashSet<>();
        if (ids != null) {
            ids.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .forEach(requested::add);
        }
        Set<String> activeIds = repository.findAll(CatalogType.TAG, false).stream()
                .map(CatalogItem::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        List<String> valid = requested.stream().filter(activeIds::contains).toList();
        List<String> unknown = requested.stream().filter(id -> !activeIds.contains(id)).toList();
        return new TagValidationResponse(unknown.isEmpty(), valid, unknown);
    }

    @Transactional(readOnly = true)
    public CatalogValidationResponse validateCategory(String id) {
        if (id == null || id.isBlank()) {
            return new CatalogValidationResponse(true);
        }
        boolean valid = repository.findById(CatalogType.CATEGORY, id.trim())
                .map(CatalogItem::active)
                .orElse(false);
        return new CatalogValidationResponse(valid);
    }

    private static String requireId(String id) {
        if (id == null || !ID_PATTERN.matcher(id.trim()).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Catalog id must use lowercase letters, digits, and hyphens"
            );
        }
        return id.trim();
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catalog name must contain 1 to 80 characters");
        }
        return name.trim();
    }
}
