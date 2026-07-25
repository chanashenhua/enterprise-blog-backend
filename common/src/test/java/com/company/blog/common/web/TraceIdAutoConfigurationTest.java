package com.company.blog.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class TraceIdAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TraceIdAutoConfiguration.class));

    @Test
    void registersTraceFilterForServletApplications() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(TraceIdFilter.class)
        );
    }
}
