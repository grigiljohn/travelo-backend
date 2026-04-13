package com.travelo.musicservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 client for the music catalog bucket ({@code music.aws.*}), separate from {@code media.storage.s3}.
 */
@Configuration
public class MusicS3BeansConfiguration {

    @Bean(name = "musicS3Client")
    public S3Client musicS3Client(MusicAwsProperties awsProperties) {
        var builder = S3Client.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (awsProperties.isCrossRegionAccessEnabled()) {
            builder.crossRegionAccessEnabled(true);
        }
        return builder.build();
    }

    @Bean(name = "musicS3Presigner")
    public S3Presigner musicS3Presigner(MusicAwsProperties awsProperties) {
        return S3Presigner.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
