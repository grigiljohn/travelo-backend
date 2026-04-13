package com.travelo.commons.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Health indicator for Kafka connectivity.
 * Uses reflection to avoid hard dependency on Kafka client in commons library.
 */
public class KafkaHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(KafkaHealthIndicator.class);
    private final String bootstrapServers;

    public KafkaHealthIndicator(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    public Health health() {
        try {
            // Use reflection to avoid hard dependency on Kafka classes
            Class<?> adminClientConfigClass = Class.forName("org.apache.kafka.clients.admin.AdminClientConfig");
            Class<?> adminClientClass = Class.forName("org.apache.kafka.clients.admin.AdminClient");
            Class<?> listTopicsResultClass = Class.forName("org.apache.kafka.clients.admin.ListTopicsResult");

            Properties props = new Properties();
            props.put(adminClientConfigClass.getField("BOOTSTRAP_SERVERS_CONFIG").get(null), bootstrapServers);
            props.put(adminClientConfigClass.getField("REQUEST_TIMEOUT_MS_CONFIG").get(null), 2000); // 2 seconds
            props.put(adminClientConfigClass.getField("CONNECTIONS_MAX_IDLE_MS_CONFIG").get(null), 10000);

            // Create AdminClient
            Method createMethod = adminClientClass.getMethod("create", Properties.class);
            Object adminClient = createMethod.invoke(null, props);

            try {
                // List topics
                Method listTopicsMethod = adminClientClass.getMethod("listTopics");
                Object topicsResult = listTopicsMethod.invoke(adminClient);
                
                // Get topic names
                Method namesMethod = listTopicsResultClass.getMethod("names");
                Object namesFuture = namesMethod.invoke(topicsResult);
                
                // Get result with timeout (reduced to 2 seconds to fail fast)
                Method getMethod = namesFuture.getClass().getMethod("get", long.class, TimeUnit.class);
                getMethod.invoke(namesFuture, 2L, TimeUnit.SECONDS);
                
                // Close admin client
                Method closeMethod = adminClientClass.getMethod("close");
                closeMethod.invoke(adminClient);
                
                return Health.up()
                        .withDetail("kafka", "connected")
                        .withDetail("bootstrapServers", bootstrapServers)
                        .withDetail("status", "ok")
                        .build();
            } catch (Exception e) {
                // Try to close admin client
                try {
                    Method closeMethod = adminClientClass.getMethod("close");
                    closeMethod.invoke(adminClient);
                } catch (Exception closeEx) {
                    logger.debug("Error closing admin client", closeEx);
                }
                throw e;
            }
        } catch (ClassNotFoundException e) {
            logger.warn("Kafka client classes not found. Kafka health check unavailable.");
            return Health.unknown()
                    .withDetail("kafka", "health check unavailable")
                    .withDetail("reason", "Kafka client not in classpath")
                    .build();
        } catch (InvocationTargetException e) {
            // Unwrap the actual exception from reflection
            Throwable cause = e.getCause();
            if (cause instanceof TimeoutException) {
                logger.warn("Kafka health check timed out - Kafka may be unavailable (non-critical)");
                // Return UNKNOWN instead of DOWN to prevent Eureka from marking service as DOWN
                // Kafka is optional for messaging-service to function
                return Health.unknown()
                        .withDetail("kafka", "connection timeout")
                        .withDetail("bootstrapServers", bootstrapServers)
                        .withDetail("error", "Connection timeout - Kafka may be unavailable")
                        .withDetail("note", "Kafka unavailability does not affect core messaging functionality")
                        .build();
            }
            // Re-throw as generic exception to be handled below
            throw new RuntimeException("Kafka health check failed", cause != null ? cause : e);
        } catch (Exception e) {
            // Check if the exception or its cause is a TimeoutException
            Throwable cause = e.getCause();
            if (e instanceof TimeoutException || cause instanceof TimeoutException) {
                logger.warn("Kafka health check timed out - Kafka may be unavailable (non-critical)");
                return Health.unknown()
                        .withDetail("kafka", "connection timeout")
                        .withDetail("bootstrapServers", bootstrapServers)
                        .withDetail("error", "Connection timeout - Kafka may be unavailable")
                        .withDetail("note", "Kafka unavailability does not affect core messaging functionality")
                        .build();
            }
            logger.warn("Kafka health check failed (non-critical): {}", e.getMessage());
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + " - Kafka connection failed";
            }
            // Return UNKNOWN instead of DOWN to prevent Eureka from marking service as DOWN
            // Kafka is optional for messaging-service to function
            return Health.unknown()
                    .withDetail("kafka", "connection failed")
                    .withDetail("bootstrapServers", bootstrapServers)
                    .withDetail("error", errorMessage)
                    .withDetail("note", "Kafka unavailability does not affect core messaging functionality")
                    .build();
        }
    }
}

