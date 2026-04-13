package com.travelo.apigateway.filter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Ensures every request routed through the gateway carries W3C trace headers so that
 * downstream services participate in the same distributed trace even when the client
 * did not send trace context.
 */
@Component
public class TracePropagationFilter implements GlobalFilter, Ordered {

    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String TRACESTATE_HEADER = "tracestate";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        boolean hasTraceparent = request.getHeaders().containsKey(TRACEPARENT_HEADER);
        boolean hasTracestate = request.getHeaders().containsKey(TRACESTATE_HEADER);

        if (hasTraceparent && hasTracestate) {
            return chain.filter(exchange);
        }

        SpanContext resolvedContext = resolveSpanContext();

        ServerHttpRequest.Builder builder = request.mutate();
        if (!hasTraceparent) {
            builder.headers(httpHeaders ->
                    httpHeaders.set(TRACEPARENT_HEADER, formatTraceparent(resolvedContext)));
        }
        if (!hasTracestate) {
            String tracestate = resolvedContext.getTraceState().isEmpty()
                    ? null
                    : resolvedContext.getTraceState().toString();
            if (tracestate != null && !tracestate.isBlank()) {
                builder.headers(httpHeaders -> httpHeaders.set(TRACESTATE_HEADER, tracestate));
            }
        }

        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    private SpanContext resolveSpanContext() {
        SpanContext current = Span.current().getSpanContext();
        if (current.isValid()) {
            return current;
        }
        return SpanContext.create(generateTraceId(), generateSpanId(), TraceFlags.getSampled(), TraceState.getDefault());
    }

    private String formatTraceparent(SpanContext spanContext) {
        return String.format("00-%s-%s-01", spanContext.getTraceId(), spanContext.getSpanId());
    }

    private String generateTraceId() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return String.format("%016x%016x", random.nextLong(), random.nextLong());
    }

    private String generateSpanId() {
        return String.format("%016x", ThreadLocalRandom.current().nextLong());
    }

    @Override
    public int getOrder() {
        // Ensure this runs after the core tracing instrumentation so we can reuse the current span.
        return Ordered.LOWEST_PRECEDENCE;
    }
}

