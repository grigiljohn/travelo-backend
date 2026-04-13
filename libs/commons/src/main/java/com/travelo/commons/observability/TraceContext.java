package com.travelo.commons.observability;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Distributed tracing context utility.
 * Manages trace ID and span ID for request correlation.
 */
public final class TraceContext {

    private static final String TRACE_ID_KEY = "trace_id";
    private static final String SPAN_ID_KEY = "span_id";
    private static final ThreadLocal<String> currentTraceId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentSpanId = new ThreadLocal<>();

    private TraceContext() {
    }

    /**
     * Initialize trace context for the current request.
     */
    public static void initTrace(String traceId) {
        String spanId = UUID.randomUUID().toString().substring(0, 8);
        setTraceId(traceId != null ? traceId : UUID.randomUUID().toString());
        setSpanId(spanId);
    }

    /**
     * Initialize trace context with new trace ID.
     */
    public static void initTrace() {
        initTrace(null);
    }

    /**
     * Create a child span (new span ID, same trace ID).
     */
    public static void createChildSpan() {
        String newSpanId = UUID.randomUUID().toString().substring(0, 8);
        setSpanId(newSpanId);
    }

    /**
     * Get current trace ID.
     */
    public static String getTraceId() {
        return currentTraceId.get();
    }

    /**
     * Get current span ID.
     */
    public static String getSpanId() {
        return currentSpanId.get();
    }

    /**
     * Set trace ID and update MDC.
     */
    private static void setTraceId(String traceId) {
        currentTraceId.set(traceId);
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * Set span ID and update MDC.
     */
    private static void setSpanId(String spanId) {
        currentSpanId.set(spanId);
        MDC.put(SPAN_ID_KEY, spanId);
    }

    /**
     * Clear trace context.
     */
    public static void clear() {
        currentTraceId.remove();
        currentSpanId.remove();
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
    }

    /**
     * Copy trace context from parent (for async operations).
     */
    public static TraceContextSnapshot snapshot() {
        return new TraceContextSnapshot(getTraceId(), getSpanId());
    }

    /**
     * Restore trace context from snapshot.
     */
    public static void restore(TraceContextSnapshot snapshot) {
        if (snapshot != null) {
            setTraceId(snapshot.getTraceId());
            setSpanId(snapshot.getSpanId());
        }
    }

    /**
     * Snapshot of trace context for async operations.
     */
    public static class TraceContextSnapshot {
        private final String traceId;
        private final String spanId;

        public TraceContextSnapshot(String traceId, String spanId) {
            this.traceId = traceId;
            this.spanId = spanId;
        }

        public String getTraceId() {
            return traceId;
        }

        public String getSpanId() {
            return spanId;
        }
    }
}

