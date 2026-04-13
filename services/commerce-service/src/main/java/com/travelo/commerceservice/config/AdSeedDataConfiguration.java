package com.travelo.commerceservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class AdSeedDataConfiguration {

    @Bean
    @DependsOn("adEntityManagerFactory")
    public DataSourceInitializer adSeedDataInitializer(
            @Qualifier("adDataSource") DataSource adDataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/ad-seed-data.sql"));
        populator.setContinueOnError(true);
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(adDataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
