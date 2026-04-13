package com.travelo.commons.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Configuration for resilient WebClient with circuit breakers, retries, and timeouts.
 */
@Configuration
public class ResilientWebClientConfig {

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired(required = false)
    private RetryRegistry retryRegistry;

    @Bean
    public WebClient.Builder resilientWebClientBuilder() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector())
                .filter(logRequest())
                .filter(logResponse())
                .filter(addTraceHeaders());
    }

    /**
     * Create a resilient WebClient with circuit breaker and retry.
     */
    public WebClient createResilientWebClient(String serviceName, String baseUrl) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry != null
                ? circuitBreakerRegistry.circuitBreaker(serviceName, serviceName)
                : null;

        Retry retry = retryRegistry != null
                ? retryRegistry.retry(serviceName, serviceName)
                : null;

        WebClient.Builder builder = resilientWebClientBuilder().baseUrl(baseUrl != null ? baseUrl : "");

        // Apply circuit breaker and retry if available
        if (circuitBreaker != null) {
            builder.filter(circuitBreakerFilter(circuitBreaker));
        }
        if (retry != null) {
            builder.filter(retryFilter(retry));
        }

        return builder.build();
    }

    private ExchangeFilterFunction circuitBreakerFilter(CircuitBreaker circuitBreaker) {
        return (request, next) -> next.exchange(request)
                .transform(CircuitBreakerOperator.of(circuitBreaker));
    }

    private ExchangeFilterFunction retryFilter(Retry retry) {
        return (request, next) -> next.exchange(request)
                .transform(RetryOperator.of(retry));
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (clientRequest.logPrefix().contains("DEBUG") || clientRequest.logPrefix().contains("TRACE")) {
                org.slf4j.LoggerFactory.getLogger(ResilientWebClientConfig.class)
                        .debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            }
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                org.slf4j.LoggerFactory.getLogger(ResilientWebClientConfig.class)
                        .warn("Response status: {}", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }

    private ExchangeFilterFunction addTraceHeaders() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            String traceId = com.travelo.commons.observability.TraceContext.getTraceId();
            if (traceId != null) {
                return Mono.just(org.springframework.web.reactive.function.client.ClientRequest.from(clientRequest)
                        .header("X-Trace-Id", traceId)
                        .header("X-Span-Id", com.travelo.commons.observability.TraceContext.getSpanId())
                        .build());
            }
            return Mono.just(clientRequest);
        });
    }
}

