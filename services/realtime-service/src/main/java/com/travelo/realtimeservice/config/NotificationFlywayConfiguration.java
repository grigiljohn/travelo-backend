package com.travelo.realtimeservice.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class NotificationFlywayConfiguration {

    @Bean
    public Flyway notificationFlyway(
            @Qualifier("notificationDataSource") DataSource notificationDataSource,
            @Value("${realtime.notification.flyway-schema:public}") String flywaySchema) {
        FluentConfiguration cfg = Flyway.configure()
                .dataSource(notificationDataSource)
                .locations("classpath:db/migration/notification")
                .baselineOnMigrate(true)
                .validateOnMigrate(false);

        // Use existing schema (default: `public`). Do NOT attempt to create schemas automatically,
        // because the DB role in local dev may not have CREATE privileges.
        if (flywaySchema != null && !flywaySchema.isBlank()) {
            String s = flywaySchema.trim();
            cfg = cfg.schemas(s).defaultSchema(s);
        }
        Flyway flyway = cfg.load();
        flyway.migrate();
        return flyway;
    }
}
