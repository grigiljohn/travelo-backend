package com.travelo.commons.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Creates databases before DataSource beans are initialized.
 * This runs very early in the Spring Boot lifecycle.
 */
@org.springframework.context.annotation.Configuration
@ConditionalOnProperty(name = "app.database.auto-create", havingValue = "true", matchIfMissing = true)
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class DatabaseInitializerBeanPostProcessor implements BeanFactoryPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializerBeanPostProcessor.class);
    private static volatile boolean initialized = false;

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (initialized) {
            return;
        }
        synchronized (DatabaseInitializerBeanPostProcessor.class) {
            if (initialized) {
                return;
            }

            try {
                // Access Environment - it should be available as a singleton
                Environment env;
                try {
                    env = beanFactory.getBean(Environment.class);
                } catch (Exception ex) {
                    logger.warn("Environment not available in BeanFactoryPostProcessor, using system properties/environment variables");
                    // Try to use system properties and environment variables directly
                    initializeFromSystemProperties();
                    initialized = true;
                    return;
                }
                
                initializeDatabases(env);
                initialized = true;
            } catch (Exception e) {
                logger.warn("Failed to initialize databases in BeanFactoryPostProcessor: {}", e.getMessage());
                logger.debug("Will retry database initialization during ApplicationRunner phase", e);
                // Don't fail - ApplicationRunner will handle it
            }
        }
    }
    
    private void initializeFromSystemProperties() {
        // Try to get datasource URL from system properties or environment variables
        String url = System.getProperty("spring.datasource.url");
        if (url == null || url.isEmpty()) {
            url = System.getenv("POSTGRES_URL");
        }
        if (url == null || url.isEmpty()) {
            url = System.getenv("DATASOURCE_URL");
        }
        if (url == null || url.isEmpty()) {
            logger.debug("No datasource URL found, skipping database initialization");
            return;
        }
        
        String username = System.getProperty("spring.datasource.username");
        if (username == null || username.isEmpty()) {
            username = System.getenv("POSTGRES_USERNAME");
        }
        if (username == null || username.isEmpty()) {
            username = System.getenv("DATASOURCE_USERNAME");
        }
        if (username == null || username.isEmpty()) {
            username = System.getenv("POSTGRES_USER");
        }
        if (username == null || username.isEmpty()) {
            username = "travelo";
        }
        
        String password = System.getProperty("spring.datasource.password");
        if (password == null || password.isEmpty()) {
            password = System.getenv("POSTGRES_PASSWORD");
        }
        if (password == null || password.isEmpty()) {
            password = System.getenv("DATASOURCE_PASSWORD");
        }
        if (password == null || password.isEmpty()) {
            password = "travelo";
        }
        
        initializeDatabase(url, username, password);
    }
    
    private void initializeDatabase(String url, String username, String password) {
        // Same logic as initializeDatabases but with direct parameters
        if (!url.startsWith("jdbc:postgresql://")) {
            return;
        }
        
        try {
            String databaseName = extractDatabaseName(url);
            if (databaseName == null || databaseName.isEmpty() || "postgres".equals(databaseName)) {
                return;
            }
            
            String postgresUrl = replaceDatabaseName(url, "postgres");
            logger.info("Checking if database '{}' exists...", databaseName);
            
            Class.forName("org.postgresql.Driver");
            
            try (Connection conn = DriverManager.getConnection(postgresUrl, username, password)) {
                String checkSql = "SELECT 1 FROM pg_database WHERE datname = ?";
                boolean exists = false;
                
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, databaseName);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        exists = rs.next();
                    }
                }
                
                if (exists) {
                    logger.info("Database '{}' already exists", databaseName);
                } else {
                    logger.info("Database '{}' does not exist. Creating...", databaseName);
                    
                    if (!isValidDatabaseName(databaseName)) {
                        throw new IllegalArgumentException("Invalid database name: " + databaseName);
                    }
                    
                    String escapedDbName = databaseName.replace("\"", "\"\"");
                    try (java.sql.Statement createStmt = conn.createStatement()) {
                        createStmt.executeUpdate("CREATE DATABASE \"" + escapedDbName + "\"");
                    }
                    
                    logger.info("Successfully created database '{}'", databaseName);
                }
            }
        } catch (ClassNotFoundException e) {
            logger.warn("PostgreSQL driver not found. Database auto-creation skipped.");
        } catch (Exception e) {
            logger.error("Failed to create database: {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private void initializeDatabases(Environment env) {
        String datasourceUrl = env.getProperty("spring.datasource.url");
        if (datasourceUrl == null || datasourceUrl.isEmpty()) {
            logger.debug("No datasource URL configured, skipping database initialization");
            return;
        }

        // Only handle PostgreSQL databases
        if (!datasourceUrl.startsWith("jdbc:postgresql://")) {
            logger.debug("Not a PostgreSQL database, skipping initialization: {}", datasourceUrl);
            return;
        }

        try {
            String databaseName = extractDatabaseName(datasourceUrl);
            if (databaseName == null || databaseName.isEmpty() || "postgres".equals(databaseName)) {
                logger.debug("Invalid or default database name, skipping: {}", databaseName);
                return;
            }

            String username = env.getProperty("spring.datasource.username", 
                env.getProperty("DATASOURCE_USERNAME", 
                env.getProperty("POSTGRES_USERNAME", "travelo")));
            String password = env.getProperty("spring.datasource.password",
                env.getProperty("DATASOURCE_PASSWORD",
                env.getProperty("POSTGRES_PASSWORD", "travelo")));

            // Connect to PostgreSQL default database to create target database
            String postgresUrl = replaceDatabaseName(datasourceUrl, "postgres");
            
            logger.info("Checking if database '{}' exists...", databaseName);

            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            try (Connection conn = DriverManager.getConnection(postgresUrl, username, password)) {
                // Check if database exists
                String checkSql = "SELECT 1 FROM pg_database WHERE datname = ?";
                boolean exists = false;
                
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, databaseName);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        exists = rs.next();
                    }
                }

                if (exists) {
                    logger.info("Database '{}' already exists", databaseName);
                } else {
                    logger.info("Database '{}' does not exist. Creating...", databaseName);
                    
                    // Validate database name
                    if (!isValidDatabaseName(databaseName)) {
                        throw new IllegalArgumentException("Invalid database name: " + databaseName);
                    }
                    
                    // Create database
                    String escapedDbName = databaseName.replace("\"", "\"\"");
                    try (java.sql.Statement createStmt = conn.createStatement()) {
                        createStmt.executeUpdate("CREATE DATABASE \"" + escapedDbName + "\"");
                    }
                    
                    logger.info("Successfully created database '{}'", databaseName);
                }
            }
        } catch (ClassNotFoundException e) {
            logger.warn("PostgreSQL driver not found. Database auto-creation skipped. " +
                       "Add postgresql driver dependency if you need auto-creation.");
        } catch (Exception e) {
            logger.error("Failed to initialize database. Application may fail to start.", e);
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private String extractDatabaseName(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            int protocolEnd = url.indexOf("://");
            if (protocolEnd == -1) {
                return null;
            }

            int startIndex = protocolEnd + 3;
            int lastSlash = url.indexOf('/', startIndex);
            if (lastSlash == -1) {
                return null;
            }

            String afterSlash = url.substring(lastSlash + 1);
            int questionMark = afterSlash.indexOf('?');
            
            if (questionMark > 0) {
                return afterSlash.substring(0, questionMark);
            } else {
                return afterSlash;
            }
        } catch (Exception e) {
            logger.warn("Could not extract database name from URL: {}", url, e);
            return null;
        }
    }

    private String replaceDatabaseName(String url, String newDatabaseName) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        try {
            int protocolEnd = url.indexOf("://");
            if (protocolEnd == -1) {
                return url;
            }

            int startIndex = protocolEnd + 3;
            int lastSlash = url.indexOf('/', startIndex);
            if (lastSlash == -1) {
                return url;
            }

            int questionMark = url.indexOf('?', lastSlash);
            if (questionMark > 0) {
                return url.substring(0, lastSlash + 1) + newDatabaseName + url.substring(questionMark);
            } else {
                return url.substring(0, lastSlash + 1) + newDatabaseName;
            }
        } catch (Exception e) {
            logger.warn("Could not replace database name in URL: {}", url, e);
            return url;
        }
    }

    private boolean isValidDatabaseName(String dbName) {
        if (dbName == null || dbName.isEmpty() || dbName.length() > 63) {
            return false;
        }
        return dbName.matches("^[a-zA-Z][a-zA-Z0-9_]*$");
    }
}

