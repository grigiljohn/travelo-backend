package com.travelo.realtimeservice.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class RealtimeDataSourcesConfiguration {

    @Bean
    @Primary
    @DependsOn(RealtimePostgresDatabaseEnsurerConfiguration.BEAN_NAME)
    @ConfigurationProperties(prefix = "realtime.datasource.messaging")
    public DataSource messagingDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @DependsOn(RealtimePostgresDatabaseEnsurerConfiguration.BEAN_NAME)
    @ConfigurationProperties(prefix = "realtime.datasource.notification")
    public DataSource notificationDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }
}
