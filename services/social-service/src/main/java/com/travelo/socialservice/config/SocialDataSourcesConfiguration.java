package com.travelo.socialservice.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class SocialDataSourcesConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "social.datasource.post")
    public DataSource postDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties(prefix = "social.datasource.story")
    public DataSource storyDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties(prefix = "social.datasource.reel")
    public DataSource reelDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }
}
