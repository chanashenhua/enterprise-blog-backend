package com.company.blog.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {
    @Test
    void reusesInboundTraceIdAndClearsMdcAfterRequest() throws Exception {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInChain = new AtomicReference<>();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-1");

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                traceIdInChain.set(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)));

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("trace-1");
        assertThat(traceIdInChain.get()).isEqualTo("trace-1");
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void generatesTraceIdWhenRequestHeaderIsBlank() throws Exception {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInChain = new AtomicReference<>();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, " ");

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                traceIdInChain.set(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)));

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo(traceIdInChain.get());
        assertThat(UUID.fromString(traceIdInChain.get())).isNotNull();
    }
}