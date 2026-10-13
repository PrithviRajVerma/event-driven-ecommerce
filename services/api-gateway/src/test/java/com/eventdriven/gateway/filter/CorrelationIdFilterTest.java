package com.eventdriven.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Generates and sets correlation ID when not present in request")
    void doFilter_NoCorrelationId_GeneratesNew() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedId).isNotNull().isNotBlank();
        assertThat(request.getAttribute(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(generatedId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Preserves existing correlation ID from request header")
    void doFilter_WithCorrelationId_Preserves() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String existingId = "custom-trace-id-12345";
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(existingId);
        assertThat(request.getAttribute(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(existingId);
        verify(filterChain).doFilter(request, response);
    }
}
