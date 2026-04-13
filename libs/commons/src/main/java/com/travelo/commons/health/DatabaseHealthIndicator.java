package com.travelo.commons.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Custom health indicator for database connectivity.
 */
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    private final JdbcTemplate jdbcTemplate;
    private final String databaseName;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate, String databaseName) {
        this.jdbcTemplate = jdbcTemplate;
        this.databaseName = databaseName;
    }

    @Override
    public Health health() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                return Health.up()
                        .withDetail("database", databaseName)
                        .withDetail("status", "connected")
                        .build();
            } else {
                return Health.down()
                        .withDetail("database", databaseName)
                        .withDetail("error", "Unexpected query result")
                        .build();
            }
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            return Health.down()
                    .withDetail("database", databaseName)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

