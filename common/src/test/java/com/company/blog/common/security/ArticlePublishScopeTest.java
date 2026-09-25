package com.company.blog.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ArticlePublishScopeTest {
    @Test void validatesShapeLimitsAndOrganizationKinds() {
        assertThat(ArticlePublishScope.validationError("company", Set.of())).isNull();
        assertThat(ArticlePublishScope.validationError("TEAM", Set.of("t-a"))).isNull();
        assertThat(ArticlePublishScope.validationError("COMPANY", Set.of("t-a"))).isNotNull();
        assertThat(ArticlePublishScope.validationError("TEAM", Set.of())).isNotNull();
        assertThat(ArticlePublishScope.validationError(null, Set.of())).isNotNull();
        assertThat(ArticlePublishScope.validationError("TEAM", Set.of(" t-a"))).isNotNull();
        assertThat(ArticlePublishScope.validationError("TEAM", Set.of("x".repeat(65)))).isNotNull();
        assertThat(ArticlePublishScope.validationError("TEAM", IntStream.range(0, 51).mapToObj(i -> "t-" + i).collect(Collectors.toSet()))).isNotNull();
    }
}
