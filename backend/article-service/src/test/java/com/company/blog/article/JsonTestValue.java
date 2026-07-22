package com.company.blog.article;

import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonTestValue {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestValue() {
    }

    static String extractString(String json, String fieldName) {
        try {
            return OBJECT_MAPPER.readTree(json).path(fieldName).asText();
        } catch (Exception ex) {
            throw new AssertionError("Unable to extract JSON field: " + fieldName, ex);
        }
    }
}