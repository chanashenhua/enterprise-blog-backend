package com.company.blog.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class MockOidcUserContextFilterProfileTest {
    @Test
    void registersFilterOnlyForDevProfile() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("dev");
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test", Map.of("blog.mock-oidc.token", "test-token"))
            );
            context.register(MockOidcUserContextFilter.class);
            context.refresh();

            assertThat(context.getBeansOfType(MockOidcUserContextFilter.class)).hasSize(1);
        }
    }

    @Test
    void excludesFilterForNonDevProfile() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod");
            context.register(MockOidcUserContextFilter.class);
            context.refresh();

            assertThat(context.getBeansOfType(MockOidcUserContextFilter.class)).isEmpty();
        }
    }
}
