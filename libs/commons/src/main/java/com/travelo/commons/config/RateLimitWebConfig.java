package com.travelo.commons.config;

import com.travelo.commons.middleware.RateLimitInterceptor;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

/**
 * Configuration for rate limiting interceptor.
 */
@Configuration
public class RateLimitWebConfig implements WebMvcConfigurer {

    private final Map<String, Bucket> rateLimitBuckets;

    public RateLimitWebConfig(Map<String, Bucket> rateLimitBuckets) {
        this.rateLimitBuckets = rateLimitBuckets;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(rateLimitBuckets))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**", "/health", "/swagger-ui/**", "/v3/api-docs/**");
    }
}

