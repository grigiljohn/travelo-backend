package com.travelo.commons.config;

import com.travelo.commons.observability.TraceContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter to initialize and propagate trace context.
 * Reads trace_id from X-Trace-Id header or generates new one.
 */
@Component
@Order(1)
public class TraceFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TraceFilter.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
                TraceContext.initTrace(traceId);
                logger.debug("Initialized trace context: traceId={}", TraceContext.getTraceId());
            }

            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}

