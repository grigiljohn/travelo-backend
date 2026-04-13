package com.travelo.socialservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class PostFlywayConfiguration {

    @Bean
    public Flyway postFlyway(@Qualifier("postDataSource") DataSource postDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(postDataSource)
                .locations("classpath:db/migration/post")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(false)
                .load();
        flyway.migrate();
        return flyway;
    }
}
