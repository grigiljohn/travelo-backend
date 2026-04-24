package com.travelo.userservice.event;

import com.travelo.userservice.dto.events.UserFollowedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Centralised publisher for user-lifecycle Kafka events.
 *
 * <p>Kafka is treated as an optional dependency — if {@code app.user.events.kafka-enabled}
 * is false (default) or the auto-configured {@link KafkaTemplate} is absent, calls become
 * no-ops. This mirrors the {@code social-service} {@code PostEventPublisher} pattern so the
 * service remains runnable locally / in tests without a broker.
 */
@Component
public class UserEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(UserEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean enabled;
    private final String topicUserFollowed;
    private final String topicUserUnfollowed;

    public UserEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.user.events.kafka-enabled:false}") boolean enabled,
            @Value("${app.user.events.topics.user-followed:user.followed}") String topicUserFollowed,
            @Value("${app.user.events.topics.user-unfollowed:user.unfollowed}") String topicUserUnfollowed) {
        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
        this.topicUserFollowed = topicUserFollowed;
        this.topicUserUnfollowed = topicUserUnfollowed;
        logger.info("UserEventPublisher initialized (enabled={}, kafka={})",
                enabled, kafkaTemplate != null);
    }

    public void publishUserFollowed(UserFollowedEvent event) {
        String topic = "unfollowed".equals(event.action()) ? topicUserUnfollowed : topicUserFollowed;
        // Key by followee so all events for a given user land on the same partition;
        // keeps per-target ordering for downstream consumers.
        send(topic, event.followeeId(), event);
    }

    private void send(String topic, String key, Object payload) {
        if (!enabled || kafkaTemplate == null) {
            logger.debug("flow=user_event_skipped topic={} key={} reason={}",
                    topic, key, !enabled ? "disabled" : "no_template");
            return;
        }
        try {
            kafkaTemplate.send(topic, key, payload);
            logger.debug("flow=user_event_published topic={} key={}", topic, key);
        } catch (Exception ex) {
            logger.warn("flow=user_event_publish_failed topic={} key={} err={}",
                    topic, key, ex.toString());
        }
    }
}
