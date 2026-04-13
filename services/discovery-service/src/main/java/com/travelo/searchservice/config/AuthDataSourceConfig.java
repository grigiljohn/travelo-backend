package com.travelo.searchservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Configuration for accessing identity (auth) database for user reindexing.
 * Only used for reindexing operations.
 */
@Configuration
public class AuthDataSourceConfig {

    @Value("${app.auth-db.url:jdbc:postgresql://localhost:5432/travelo_auth}")
    private String dbUrl;

    @Value("${app.auth-db.username:travelo}")
    private String dbUsername;

    @Value("${app.auth-db.password:travelo}")
    private String dbPassword;

    @Bean(name = "authDataSource")
    public DataSource authDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);
        return dataSource;
    }

    @Bean(name = "authJdbcTemplate")
    public JdbcTemplate authJdbcTemplate() {
        return new JdbcTemplate(authDataSource());
    }
}

