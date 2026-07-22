package com.company.blog.article.api;

import java.util.List;

record TagValidationResponse(boolean valid, List<String> validIds, List<String> unknownIds) {
}