package com.travelo.notificationservice.config;

import com.travelo.commons.health.DatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Production configuration for notifications (realtime-service).
 * Kafka and rate-limit beans live in {@link com.travelo.messagingservice.config.MessagingServiceProductionConfig}.
 */
@Configuration
public class NotificationServiceProductionConfig {

    @Bean(name = "notificationDatabaseHealthIndicator")
    public DatabaseHealthIndicator notificationDatabaseHealthIndicator(
            @Qualifier("notificationDataSource") DataSource notificationDataSource,
            @Value("${realtime.datasource.notification.jdbc-url:${realtime.datasource.notification.url:}}") String datasourceUrl) {
        String dbName = extractDatabaseName(datasourceUrl);
        return new DatabaseHealthIndicator(new JdbcTemplate(notificationDataSource), dbName);
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
