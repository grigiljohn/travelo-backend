package com.travelo.realtimeservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Connects to the {@code postgres} maintenance database and creates application databases if missing.
 * Requires {@code CREATEDB} (or superuser) for the configured users. Disable in locked-down prod via
 * {@code realtime.datasource.auto-create-databases=false}.
 */
@Configuration
public class RealtimePostgresDatabaseEnsurerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RealtimePostgresDatabaseEnsurerConfiguration.class);

    public static final String BEAN_NAME = "realtimePostgresDatabasesEnsured";

    @Bean(BEAN_NAME)
    public String realtimePostgresDatabasesEnsured(
            @Value("${realtime.datasource.auto-create-databases:true}") boolean enabled,
            @Value("${realtime.datasource.messaging.jdbc-url:${realtime.datasource.messaging.url:}}") String messagingUrl,
            @Value("${realtime.datasource.messaging.username}") String messagingUser,
            @Value("${realtime.datasource.messaging.password}") String messagingPassword,
            @Value("${realtime.datasource.notification.jdbc-url:${realtime.datasource.notification.url:}}")
                    String notificationUrl,
            @Value("${realtime.datasource.notification.username}") String notificationUser,
            @Value("${realtime.datasource.notification.password}") String notificationPassword) {
        if (!enabled) {
            log.debug("Skipping PostgreSQL database auto-create (realtime.datasource.auto-create-databases=false)");
            return BEAN_NAME;
        }
        Set<String> done = new HashSet<>();
        ensure(messagingUrl, messagingUser, messagingPassword, done);
        ensure(notificationUrl, notificationUser, notificationPassword, done);
        return BEAN_NAME;
    }

    private static void ensure(String jdbcUrl, String user, String password, Set<String> done) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return;
        }
        String db = extractDatabaseName(jdbcUrl);
        if (db == null || db.isEmpty() || "postgres".equalsIgnoreCase(db)) {
            return;
        }
        if (!db.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
            log.warn("Skipping auto-create for database name with unsupported characters: {}", db);
            return;
        }
        String key = maintenanceUrlKey(jdbcUrl) + "|" + db.toLowerCase(Locale.ROOT);
        if (!done.add(key)) {
            return;
        }
        String maintenanceUrl = replaceDatabaseInUrl(jdbcUrl, "postgres");
        try (Connection c = DriverManager.getConnection(maintenanceUrl, user, password)) {
            if (databaseExists(c, db)) {
                log.debug("PostgreSQL database '{}' already exists", db);
                return;
            }
            try (Statement st = c.createStatement()) {
                st.executeUpdate("CREATE DATABASE " + db);
            }
            log.info("Created PostgreSQL database '{}' for realtime-service", db);
        } catch (SQLException e) {
            if ("42P04".equals(e.getSQLState())) {
                log.debug("PostgreSQL database '{}' already exists (concurrent create)", db);
                return;
            }
            log.error(
                    "Could not auto-create database '{}'. Create it manually (CREATE DATABASE {}), grant CREATEDB to user, "
                            + "or set realtime.datasource.auto-create-databases=false. Maintenance JDBC URL: {}",
                    db, db, maintenanceUrl, e);
            throw new IllegalStateException(
                    "Missing or unreachable database '" + db + "' and auto-create failed.", e);
        }
    }

    private static boolean databaseExists(Connection c, String db) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            ps.setString(1, db);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    static String extractDatabaseName(String jdbcUrl) {
        String sansParams = jdbcUrl.split("\\?", 2)[0];
        int i = sansParams.lastIndexOf('/');
        if (i < 0 || i == sansParams.length() - 1) {
            return null;
        }
        return sansParams.substring(i + 1);
    }

    static String replaceDatabaseInUrl(String jdbcUrl, String newDb) {
        String[] parts = jdbcUrl.split("\\?", 2);
        String sansParams = parts[0];
        int i = sansParams.lastIndexOf('/');
        String base = sansParams.substring(0, i + 1);
        String suffix = parts.length > 1 ? "?" + parts[1] : "";
        return base + newDb + suffix;
    }

    private static String maintenanceUrlKey(String jdbcUrl) {
        String sansParams = jdbcUrl.split("\\?", 2)[0];
        int i = sansParams.lastIndexOf('/');
        return sansParams.substring(0, i);
    }
}
