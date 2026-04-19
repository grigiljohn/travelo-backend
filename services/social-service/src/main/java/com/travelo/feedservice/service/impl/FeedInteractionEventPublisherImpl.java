package com.travelo.feedservice.service.impl;

import com.travelo.feedservice.dto.FeedUserEventDto;
import com.travelo.feedservice.service.FeedInteractionEventPublisher;
import com.travelo.feedservice.service.FeedRealtimeSignalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FeedInteractionEventPublisherImpl implements FeedInteractionEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(FeedInteractionEventPublisherImpl.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FeedRealtimeSignalService feedRealtimeSignalService;

    @Value("${app.feed.events.kafka-enabled:false}")
    private boolean kafkaEnabled;

    @Value("${app.feed.events.kafka-topic:feed.user_events}")
    private String kafkaTopic;

    public FeedInteractionEventPublisherImpl(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            FeedRealtimeSignalService feedRealtimeSignalService) {
        this.kafkaTemplate = kafkaTemplate;
        this.feedRealtimeSignalService = feedRealtimeSignalService;
    }

    @Override
    public void publish(UUID userId, String surface, List<FeedUserEventDto> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        logger.info(
                "flow=feed_user_events userId={} surface={} count={} kafka={}",
                userId,
                surface,
                events.size(),
                kafkaEnabled && kafkaTemplate != null);

        for (FeedUserEventDto e : events) {
            logger.debug(
                    "flow=feed_user_events detail userId={} surface={} type={} itemType={} targetId={}",
                    userId,
                    surface,
                    e.getEventType(),
                    e.getItemType(),
                    e.getTargetId());
        }
        feedRealtimeSignalService.recordUserEvents(userId, surface, events);

        if (!kafkaEnabled || kafkaTemplate == null) {
            return;
        }
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("user_id", userId.toString());
        envelope.put("surface", surface);
        envelope.put("emitted_at", Instant.now().toString());
        envelope.put("events", events);
        try {
            kafkaTemplate.send(kafkaTopic, userId.toString(), envelope);
        } catch (Exception ex) {
            logger.warn("flow=feed_user_events_kafka_fail topic={} err={}", kafkaTopic, ex.toString());
        }
    }
}
