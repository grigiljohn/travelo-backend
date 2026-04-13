package com.travelo.commerceservice.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class CommerceDataSourcesConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "commerce.datasource.shop")
    public DataSource shopDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties(prefix = "commerce.datasource.ad")
    public DataSource adDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }
}
