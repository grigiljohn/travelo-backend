package com.travelo.mediaservice;

import com.travelo.mediaservice.config.LocalStorageProperties;
import com.travelo.mediaservice.config.MediaKafkaProperties;
import com.travelo.mediaservice.config.MediaPublicUrlProperties;
import com.travelo.musicservice.config.MusicAwsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.travelo.mediaservice", "com.travelo.musicservice"})
@EnableJpaRepositories(basePackages = {
        "com.travelo.mediaservice.repository",
        "com.travelo.musicservice.repository",
})
@EntityScan(basePackages = {
        "com.travelo.mediaservice.entity",
        "com.travelo.musicservice.entity",
})
@EnableDiscoveryClient
@EnableConfigurationProperties({
        LocalStorageProperties.class,
        MediaKafkaProperties.class,
        MediaPublicUrlProperties.class,
        MusicAwsProperties.class,
})
public class MediaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }
}
