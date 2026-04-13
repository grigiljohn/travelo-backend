package com.travelo.socialservice.config;

import com.travelo.commons.health.DatabaseHealthIndicator;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * JDBC and DB health for the three social databases (post / story / reel).
 * Primary {@link JdbcTemplate} targets the post database for legacy components that inject it without a qualifier.
 */
@Configuration
public class SocialMultiDataSourceJdbcConfig {

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("postDataSource") DataSource postDataSource) {
        return new JdbcTemplate(postDataSource);
    }

    @Bean
    public JdbcTemplate storyJdbcTemplate(@Qualifier("storyDataSource") DataSource storyDataSource) {
        return new JdbcTemplate(storyDataSource);
    }

    @Bean
    public JdbcTemplate reelJdbcTemplate(@Qualifier("reelDataSource") DataSource reelDataSource) {
        return new JdbcTemplate(reelDataSource);
    }

    @Bean(name = "postDatabaseHealthIndicator")
    public DatabaseHealthIndicator postDatabaseHealthIndicator(
            @Qualifier("postDataSource") DataSource postDataSource,
            JdbcTemplate jdbcTemplate) {
        return new DatabaseHealthIndicator(jdbcTemplate, databaseLabel(postDataSource, "travelo_posts"));
    }

    @Bean(name = "storyDatabaseHealthIndicator")
    public DatabaseHealthIndicator storyDatabaseHealthIndicator(
            @Qualifier("storyDataSource") DataSource storyDataSource,
            @Qualifier("storyJdbcTemplate") JdbcTemplate storyJdbcTemplate) {
        return new DatabaseHealthIndicator(storyJdbcTemplate, databaseLabel(storyDataSource, "travelo_stories"));
    }

    @Bean(name = "reelDatabaseHealthIndicator")
    public DatabaseHealthIndicator reelDatabaseHealthIndicator(
            @Qualifier("reelDataSource") DataSource reelDataSource,
            @Qualifier("reelJdbcTemplate") JdbcTemplate reelJdbcTemplate) {
        return new DatabaseHealthIndicator(reelJdbcTemplate, databaseLabel(reelDataSource, "travelo_reels"));
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
