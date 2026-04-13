package com.travelo.commons.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.lang.reflect.Method;

/**
 * Health indicator for Elasticsearch connectivity.
 * Uses reflection to avoid hard dependency on Elasticsearch client in commons library.
 */
public class ElasticsearchHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchHealthIndicator.class);
    private final Object elasticsearchClient;

    public ElasticsearchHealthIndicator(Object elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public Health health() {
        try {
            // Use reflection to call Elasticsearch client methods
            // This avoids hard dependency on Elasticsearch classes in commons library
            Method clusterMethod = elasticsearchClient.getClass().getMethod("cluster");
            Object clusterApi = clusterMethod.invoke(elasticsearchClient);
            
            Method healthMethod = clusterApi.getClass().getMethod("health");
            Object healthResponse = healthMethod.invoke(clusterApi);
            
            // Get status using reflection
            Method statusMethod = healthResponse.getClass().getMethod("status");
            Object statusObj = statusMethod.invoke(healthResponse);
            String status = statusObj.toString();
            
            // Get cluster name
            Method clusterNameMethod = healthResponse.getClass().getMethod("clusterName");
            String clusterName = (String) clusterNameMethod.invoke(healthResponse);
            
            if ("GREEN".equals(status) || "YELLOW".equals(status)) {
                Health.Builder healthBuilder = Health.up()
                        .withDetail("elasticsearch", "connected")
                        .withDetail("cluster_name", clusterName)
                        .withDetail("status", status);
                
                // Try to get additional details
                try {
                    Method nodesMethod = healthResponse.getClass().getMethod("numberOfNodes");
                    Object nodes = nodesMethod.invoke(healthResponse);
                    healthBuilder.withDetail("number_of_nodes", nodes);
                    
                    Method dataNodesMethod = healthResponse.getClass().getMethod("numberOfDataNodes");
                    Object dataNodes = dataNodesMethod.invoke(healthResponse);
                    healthBuilder.withDetail("number_of_data_nodes", dataNodes);
                } catch (Exception e) {
                    // Ignore if methods don't exist
                    logger.debug("Could not retrieve additional cluster details", e);
                }
                
                return healthBuilder.build();
            } else {
                return Health.down()
                        .withDetail("elasticsearch", "cluster status: " + status)
                        .withDetail("cluster_name", clusterName)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Elasticsearch health check failed", e);
            return Health.down()
                    .withDetail("elasticsearch", "connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

