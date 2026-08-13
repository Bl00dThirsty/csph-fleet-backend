package com.gpl.common.filter;

import com.gpl.common.constant.LoggingConstants;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to manage correlation ID in request and MDC.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    /*
     * Filters the request to add correlation ID.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpServletRequest && response instanceof HttpServletResponse httpServletResponse) {
            String correlationId = httpServletRequest.getHeader(LoggingConstants.CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put(LoggingConstants.CORRELATION_ID_MDC_KEY, correlationId);
            httpServletResponse.setHeader(LoggingConstants.CORRELATION_ID_HEADER, correlationId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(LoggingConstants.CORRELATION_ID_MDC_KEY);
        }
    }
}
