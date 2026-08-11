package com.company.blog.notification.api;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum SubscriptionTargetType {
    TAG,
    CATEGORY;

    public static SubscriptionTargetType from(String value) {
        try {
            return valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription type must be TAG or CATEGORY");
        }
    }
}
