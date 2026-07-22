package com.company.blog.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserContextTest {
    @Test
    void normalizesMissingScopesToEmptySets() {
        UserContext context = new UserContext("u-1", null, null, null);

        assertThat(context.roles()).isEmpty();
        assertThat(context.departmentIds()).isEmpty();
        assertThat(context.teamIds()).isEmpty();
    }

    @Test
    void copiesScopeSetsDefensively() {
        Set<String> roles = new HashSet<>();
        roles.add("AUTHOR");

        UserContext context = new UserContext("u-1", roles, Set.of("dept-1"), Set.of("team-1"));
        roles.add("ADMIN");

        assertThat(context.roles()).containsExactly("AUTHOR");
        assertThatThrownBy(() -> context.roles().add("REVIEWER"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}