package com.travelo.mediaservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "media.kafka")
public class MediaKafkaProperties {

    /**
     * Topic used to publish uploaded media events.
     */
    private String topic = "media.uploaded";

    /**
     * When true, registers the {@code media.uploaded} consumer. Default off for local dev without a broker.
     */
    private boolean listenersEnabled = false;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public boolean isListenersEnabled() {
        return listenersEnabled;
    }

    public void setListenersEnabled(boolean listenersEnabled) {
        this.listenersEnabled = listenersEnabled;
    }
}

