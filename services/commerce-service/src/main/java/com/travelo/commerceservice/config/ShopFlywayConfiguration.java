package com.travelo.commerceservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class ShopFlywayConfiguration {

    @Bean
    public Flyway shopFlyway(@Qualifier("shopDataSource") DataSource shopDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(shopDataSource)
                .locations("classpath:db/migration/shop")
                .baselineOnMigrate(true)
                .validateOnMigrate(false)
                .load();
        flyway.migrate();
        return flyway;
    }
}
