package com.travelo.adservice.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private final DataSource dataSource;

    public DatabaseConfig(@Qualifier("adDataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void checkDatabaseTables() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String[] tableTypes = {"TABLE"};
            ResultSet tables = metaData.getTables(null, null, "%", tableTypes);
            
            int tableCount = 0;
            logger.info("=== Database Tables Check ===");
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                logger.info("Found table: {}", tableName);
                tableCount++;
            }
            
            if (tableCount == 0) {
                logger.warn("No tables found in database. Hibernate will create tables on first entity access.");
            } else {
                logger.info("Total tables found: {}", tableCount);
            }
            logger.info("=== End Database Check ===");
        } catch (Exception e) {
            logger.error("Error checking database tables", e);
        }
    }
}

