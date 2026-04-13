package com.travelo.mediaservice.config;

import com.travelo.mediaservice.event.MediaUploadedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean(name = "mediaKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, MediaUploadedEvent> mediaKafkaListenerContainerFactory(
            ConsumerFactory<String, MediaUploadedEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, MediaUploadedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}

