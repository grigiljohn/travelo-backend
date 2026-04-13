package com.travelo.commons.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * DataSource wrapper that creates the database before establishing connection.
 * This ensures the database exists before any connection attempts.
 */
public class DatabaseInitializingDataSource implements DataSource, InitializingBean {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializingDataSource.class);
    private final DataSourceProperties properties;
    private DataSource delegate;
    private volatile boolean initialized = false;
    
    public DatabaseInitializingDataSource(DataSourceProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        if (initialized) {
            return;
        }
        
        synchronized (this) {
            if (initialized) {
                return;
            }
            
            String url = properties.getUrl();
            String username = properties.getUsername();
            String password = properties.getPassword();
            
            if (url != null && url.startsWith("jdbc:postgresql://")) {
                createDatabaseIfNotExists(url, username, password);
            }
            
            // Create the actual DataSource
            this.delegate = DataSourceBuilder.create()
                    .url(url)
                    .username(username)
                    .password(password)
                    .driverClassName(properties.getDriverClassName())
                    .build();
            
            initialized = true;
        }
    }
    
    private void createDatabaseIfNotExists(String url, String username, String password) {
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
            return questionMark > 0 ? afterSlash.substring(0, questionMark) : afterSlash;
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
    
    // DataSource delegate methods
    @Override
    public Connection getConnection() throws java.sql.SQLException {
        ensureInitialized();
        return delegate.getConnection();
    }
    
    @Override
    public Connection getConnection(String username, String password) throws java.sql.SQLException {
        ensureInitialized();
        return delegate.getConnection(username, password);
    }
    
    @Override
    public java.io.PrintWriter getLogWriter() throws java.sql.SQLException {
        ensureInitialized();
        return delegate.getLogWriter();
    }
    
    @Override
    public void setLogWriter(java.io.PrintWriter out) throws java.sql.SQLException {
        ensureInitialized();
        delegate.setLogWriter(out);
    }
    
    @Override
    public void setLoginTimeout(int seconds) throws java.sql.SQLException {
        ensureInitialized();
        delegate.setLoginTimeout(seconds);
    }
    
    @Override
    public int getLoginTimeout() throws java.sql.SQLException {
        ensureInitialized();
        return delegate.getLoginTimeout();
    }
    
    @Override
    public java.util.logging.Logger getParentLogger() {
        return java.util.logging.Logger.getLogger(getClass().getName());
    }
    
    @Override
    public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
        ensureInitialized();
        if (iface.isInstance(delegate)) {
            return iface.cast(delegate);
        }
        return delegate.unwrap(iface);
    }
    
    @Override
    public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
        ensureInitialized();
        return iface.isInstance(delegate) || delegate.isWrapperFor(iface);
    }
    
    private void ensureInitialized() {
        if (!initialized) {
            try {
                afterPropertiesSet();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize DataSource", e);
            }
        }
    }
}

