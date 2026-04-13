package com.travelo.commerceservice.config;

import com.travelo.commons.health.DatabaseHealthIndicator;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * JDBC templates and DB health for shop and ad databases (commerce-service runs both domains).
 */
@Configuration
public class CommerceMultiDataSourceJdbcConfig {

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("shopDataSource") DataSource shopDataSource) {
        return new JdbcTemplate(shopDataSource);
    }

    @Bean
    public JdbcTemplate adJdbcTemplate(@Qualifier("adDataSource") DataSource adDataSource) {
        return new JdbcTemplate(adDataSource);
    }

    @Bean(name = "shopDatabaseHealthIndicator")
    public DatabaseHealthIndicator shopDatabaseHealthIndicator(
            @Qualifier("shopDataSource") DataSource shopDataSource,
            JdbcTemplate jdbcTemplate) {
        return new DatabaseHealthIndicator(jdbcTemplate, databaseLabel(shopDataSource, "travelo_shops"));
    }

    @Bean(name = "adDatabaseHealthIndicator")
    public DatabaseHealthIndicator adDatabaseHealthIndicator(
            @Qualifier("adDataSource") DataSource adDataSource,
            @Qualifier("adJdbcTemplate") JdbcTemplate adJdbcTemplate) {
        return new DatabaseHealthIndicator(adJdbcTemplate, databaseLabel(adDataSource, "travelo_ads"));
    }

    private static String databaseLabel(DataSource dataSource, String fallback) {
        if (dataSource instanceof HikariDataSource hikari) {
            String url = hikari.getJdbcUrl();
            if (url != null && !url.isBlank()) {
                return extractDatabaseName(url);
            }
        }
        return fallback;
    }

    private static String extractDatabaseName(String url) {
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
