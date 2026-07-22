package com.company.blog.permission.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.permission.PermissionPolicy;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionControllerTest {
    @Test
    void mapsPolicyDecisionToCheckResponse() {
        PermissionController controller = new PermissionController(new PermissionPolicy());

        PermissionCheckResponse response = controller.check(new PermissionCheckRequest(
                "u-admin",
                Set.of("ADMIN"),
                Set.of(),
                Set.of(),
                "article.edit",
                "article",
                "u-author",
                "company",
                Set.of()
        ));

        assertThat(response.allowed()).isTrue();
        assertThat(response.reason()).isEqualTo("ALLOWED");
    }
}
