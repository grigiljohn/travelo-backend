package com.travelo.searchservice.config;

import com.travelo.commons.config.RateLimitConfig;
import com.travelo.commons.config.ResilientWebClientConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Production configuration for discovery-service (search).
 */
@Configuration
public class SearchServiceProductionConfig {

    // Elasticsearch health checks are auto-configured by Spring Boot when Elasticsearch is available

    @Bean
    public ResilientWebClientConfig resilientWebClientConfig() {
        return new ResilientWebClientConfig();
    }

    @Bean
    public Map<String, io.github.bucket4j.Bucket> rateLimitBuckets() {
        return RateLimitConfig.rateLimitBuckets();
    }
}

