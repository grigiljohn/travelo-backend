package com.travelo.postservice.config;

import com.travelo.commons.config.ResilientWebClientConfig;
import com.travelo.commons.config.RateLimitConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Production configuration for post-service.
 */
@Configuration
public class PostServiceProductionConfig {

    @Bean
    public ResilientWebClientConfig resilientWebClientConfig() {
        // Create bean instance - Spring will inject @Autowired dependencies
        return new ResilientWebClientConfig();
    }

    @Bean
    public Map<String, io.github.bucket4j.Bucket> rateLimitBuckets() {
        return RateLimitConfig.rateLimitBuckets();
    }
}

