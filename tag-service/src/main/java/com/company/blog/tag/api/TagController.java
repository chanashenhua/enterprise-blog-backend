package com.company.blog.tag.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/tags")
public class TagController {
    private static final Set<String> KNOWN_TAG_IDS = Set.of(
            "java",
            "spring-cloud",
            "redis",
            "postgresql",
            "elasticsearch"
    );

    @GetMapping("/validate")
    public TagValidationResponse validate(@RequestParam(name = "ids", defaultValue = "") List<String> ids) {
        LinkedHashSet<String> validIds = new LinkedHashSet<>();
        LinkedHashSet<String> unknownIds = new LinkedHashSet<>();
        for (String id : ids) {
            if (KNOWN_TAG_IDS.contains(id)) {
                validIds.add(id);
            } else if (id != null && !id.isBlank()) {
                unknownIds.add(id);
            }
        }
        return new TagValidationResponse(unknownIds.isEmpty(), List.copyOf(validIds), List.copyOf(unknownIds));
    }

}
