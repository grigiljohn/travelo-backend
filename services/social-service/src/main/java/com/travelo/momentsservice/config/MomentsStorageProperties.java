package com.travelo.momentsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "moments.storage")
public record MomentsStorageProperties(String localDir) {
}
