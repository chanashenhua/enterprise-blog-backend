package com.company.blog.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.common.security.UserContext;
import com.company.blog.search.index.ArticleSearchDocument;
import com.company.blog.search.service.SearchVisibilityFilter;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SearchVisibilityFilterTest {
    private final SearchVisibilityFilter filter = new SearchVisibilityFilter();

    @Test
    void filtersOutTeamArticleForUserOutsideTeam() {
        UserContext user = new UserContext("u-reader", Set.of("READER"), Set.of("d-pay"), Set.of("t-pay"));
        ArticleSearchDocument document = document("TEAM", Set.of("t-search"));

        assertThat(filter.isVisible(user, document)).isFalse();
    }

    @Test
    void exposesCompanyArticleToAuthenticatedUser() {
        UserContext user = new UserContext("u-reader", Set.of("READER"), Set.of(), Set.of());

        assertThat(filter.isVisible(user, document("COMPANY", Set.of()))).isTrue();
    }

    private static ArticleSearchDocument document(String visibilityType, Set<String> targets) {
        return new ArticleSearchDocument(
                "a-1", "Search safely", "summary", "plain", Set.of("search"), "u-author", "u-author",
                visibilityType, targets, "PUBLISHED", Instant.now(), Instant.now()
        );
    }
}
