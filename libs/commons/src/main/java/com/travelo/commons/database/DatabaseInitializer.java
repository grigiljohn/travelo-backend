package com.travelo.commons.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Database initializer that creates the database if it doesn't exist.
 * Runs with HIGHEST_PRECEDENCE to execute as early as possible.
 */
@Configuration
@ConditionalOnProperty(name = "app.database.auto-create", havingValue = "true", matchIfMissing = true)
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE) // Run before all other ApplicationRunners
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:${DATASOURCE_USERNAME:${POSTGRES_USERNAME:travelo}}}")
    private String username;

    @Value("${spring.datasource.password:${DATASOURCE_PASSWORD:${POSTGRES_PASSWORD:travelo}}}")
    private String password;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        createDatabaseIfNeeded();
    }
    
    public void createDatabaseIfNeeded() {
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

            // Connect to PostgreSQL default database to create target database
            String postgresUrl = replaceDatabaseName(datasourceUrl, "postgres");
            
            logger.info("Checking if database '{}' exists...", databaseName);

            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            try (Connection conn = DriverManager.getConnection(postgresUrl, username, password)) {
                // Check if database exists (using prepared statement to prevent SQL injection)
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
                    
                    // Create database - validate the database name first to prevent SQL injection
                    if (!isValidDatabaseName(databaseName)) {
                        throw new IllegalArgumentException("Invalid database name: " + databaseName);
                    }
                    
                    // Note: CREATE DATABASE cannot use prepared statements, but we've validated the name
                    // Escape double quotes in database name for safety
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
    
    public void createDatabaseIfNeeded(String url, String user, String pwd) {
        // Overloaded method for static initialization
        String originalUrl = datasourceUrl;
        String originalUser = username;
        String originalPwd = password;
        try {
            datasourceUrl = url;
            username = user;
            password = pwd;
            createDatabaseIfNeeded();
        } finally {
            datasourceUrl = originalUrl;
            username = originalUser;
            password = originalPwd;
        }
    }

    private String extractDatabaseName(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // Extract database name from JDBC URL
        // Format: jdbc:postgresql://host:port/database?params
        try {
            // Find the third slash (after jdbc:postgresql://)
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
            // Find the third slash (after jdbc:postgresql://)
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

    /**
     * Validate database name to prevent SQL injection.
     * Database names should only contain letters, numbers, and underscores.
     */
    private boolean isValidDatabaseName(String dbName) {
        if (dbName == null || dbName.isEmpty() || dbName.length() > 63) {
            return false;
        }
        // PostgreSQL database names: alphanumeric + underscore, must start with letter
        return dbName.matches("^[a-zA-Z][a-zA-Z0-9_]*$");
    }
}
