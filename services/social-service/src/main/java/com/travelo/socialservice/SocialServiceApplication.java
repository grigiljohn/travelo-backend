package com.travelo.socialservice;

import com.travelo.momentsservice.config.MomentsAiOpenAiProperties;
import com.travelo.momentsservice.config.MomentsStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        scanBasePackages = {
                "com.travelo.postservice",
                "com.travelo.feedservice",
                "com.travelo.storyservice",
                "com.travelo.momentsservice",
                "com.travelo.reelservice",
                "com.travelo.planservice",
                "com.travelo.circlesservice",
                "com.travelo.socialservice",
        },
        exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                FlywayAutoConfiguration.class,
        }
)
@EnableDiscoveryClient
@EnableTransactionManagement
@EnableScheduling
@EnableConfigurationProperties({MomentsStorageProperties.class, MomentsAiOpenAiProperties.class})
public class SocialServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialServiceApplication.class, args);
    }
}
