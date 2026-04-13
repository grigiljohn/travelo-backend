package com.travelo.mediaservice.config;

import com.travelo.commons.config.RateLimitConfig;
import com.travelo.commons.health.DatabaseHealthIndicator;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Production configuration for media-service.
 */
@Configuration
public class MediaServiceProductionConfig {

    @Bean(name = "mediaDatabaseHealthIndicator")
    public DatabaseHealthIndicator databaseHealthIndicator(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate) {
        String dbName = "travelo_media";
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            if (hikari.getJdbcUrl() != null) {
                dbName = extractDatabaseName(hikari.getJdbcUrl());
            }
        }
        return new DatabaseHealthIndicator(jdbcTemplate, dbName);
    }

    @Bean
    public Map<String, io.github.bucket4j.Bucket> rateLimitBuckets() {
        return RateLimitConfig.rateLimitBuckets();
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

