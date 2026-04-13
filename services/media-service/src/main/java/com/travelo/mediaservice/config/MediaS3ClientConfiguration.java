package com.travelo.mediaservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(MediaS3Properties.class)
public class MediaS3ClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MediaS3ClientConfiguration.class);

    @Bean(name = "mediaServiceS3Client")
    @ConditionalOnProperty(prefix = "media.storage.s3", name = "enabled", havingValue = "true")
    public S3Client mediaServiceS3Client(MediaS3Properties properties) {
        if (!StringUtils.hasText(properties.getBucket())) {
            throw new IllegalStateException(
                    "media.storage.s3.enabled=true but media.storage.s3.bucket is empty; set MEDIA_S3_BUCKET (or yaml).");
        }
        var builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (properties.isCrossRegionAccessEnabled()) {
            builder.crossRegionAccessEnabled(true);
        }
        log.info("media-service S3: bucket={}, region={}, keyPrefix={}",
                properties.getBucket(), properties.getRegion(), properties.getKeyPrefix());
        return builder.build();
    }
}
