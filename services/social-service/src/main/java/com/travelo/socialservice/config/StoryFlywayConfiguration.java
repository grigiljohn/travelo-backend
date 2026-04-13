package com.travelo.socialservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class StoryFlywayConfiguration {

    @Bean
    public Flyway storyFlyway(@Qualifier("storyDataSource") DataSource storyDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(storyDataSource)
                .locations("classpath:db/migration/story")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(false)
                .load();
        flyway.migrate();
        return flyway;
    }
}
