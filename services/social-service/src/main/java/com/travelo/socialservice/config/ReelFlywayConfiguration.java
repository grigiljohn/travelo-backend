package com.travelo.socialservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class ReelFlywayConfiguration {

    @Bean
    public Flyway reelFlyway(@Qualifier("reelDataSource") DataSource reelDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(reelDataSource)
                .locations("classpath:db/migration/reel")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(false)
                .load();
        flyway.migrate();
        return flyway;
    }
}
