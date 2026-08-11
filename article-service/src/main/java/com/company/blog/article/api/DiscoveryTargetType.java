package com.company.blog.article.api;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum DiscoveryTargetType {
    CATEGORY,
    TAG;

    public static DiscoveryTargetType from(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discovery type is required");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discovery type must be CATEGORY or TAG", exception);
        }
    }
}
