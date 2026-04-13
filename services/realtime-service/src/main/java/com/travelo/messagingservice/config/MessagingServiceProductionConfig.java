package com.travelo.messagingservice.config;

import com.travelo.commons.config.RateLimitConfig;
import com.travelo.commons.health.DatabaseHealthIndicator;
import com.travelo.commons.health.KafkaHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Production configuration for messaging (realtime-service).
 */
@Configuration
public class MessagingServiceProductionConfig {

    @Bean(name = "messagingDatabaseHealthIndicator")
    public DatabaseHealthIndicator messagingDatabaseHealthIndicator(
            @Qualifier("messagingDataSource") DataSource messagingDataSource,
            @Value("${realtime.datasource.messaging.jdbc-url:${realtime.datasource.messaging.url:}}") String datasourceUrl) {
        String dbName = extractDatabaseName(datasourceUrl);
        return new DatabaseHealthIndicator(new JdbcTemplate(messagingDataSource), dbName);
    }

    @Bean
    public KafkaHealthIndicator kafkaHealthIndicator(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        return new KafkaHealthIndicator(bootstrapServers);
    }

    @Bean
    public Map<String, io.github.bucket4j.Bucket> rateLimitBuckets() {
        return RateLimitConfig.rateLimitBuckets();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return builder
                .requestFactory(() -> factory)
                .build();
    }

    private String extractDatabaseName(String url) {
        if (url.contains("/")) {
            String[] parts = url.split("/");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                return lastPart.split("\\?")[0];
            }
        }
        return "unknown";
    }
}

