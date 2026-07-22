package com.company.blog.tag.api;

import java.util.List;

public record TagValidationResponse(boolean valid, List<String> validIds, List<String> unknownIds) {
}