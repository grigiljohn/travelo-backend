package com.travelo.realtimeservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MessagingFlywayConfiguration {

    @Bean
    public Flyway messagingFlyway(@Qualifier("messagingDataSource") DataSource messagingDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(messagingDataSource)
                .locations("classpath:db/migration/messaging")
                .baselineOnMigrate(true)
                .validateOnMigrate(false)
                .load();
        flyway.migrate();
        return flyway;
    }
}
