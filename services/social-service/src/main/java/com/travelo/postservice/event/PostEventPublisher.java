package com.travelo.postservice.event;

import com.travelo.postservice.dto.events.CommentCreatedEvent;
import com.travelo.postservice.dto.events.PostCreatedEvent;
import com.travelo.postservice.dto.events.PostLikedEvent;
import com.travelo.postservice.dto.events.TagCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Centralised publisher for post/comment/tag Kafka events.
 *
 * <p>Kafka is treated as an optional dependency — if {@code app.post.events.kafka-enabled}
 * is false (default) or the auto-configured {@link KafkaTemplate} is absent, calls become
 * no-ops. This keeps local dev + tests runnable without a broker while still allowing
 * zero-code promotion in any environment that sets the flag.
 */
@Component
public class PostEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(PostEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean enabled;
    private final String topicPostCreated;
    private final String topicPostLiked;
    private final String topicPostLikeRemoved;
    private final String topicCommentCreated;
    private final String topicTagCreated;

    public PostEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.post.events.kafka-enabled:false}") boolean enabled,
            @Value("${app.post.events.topics.post-created:post.created}") String topicPostCreated,
            @Value("${app.post.events.topics.post-liked:post.liked}") String topicPostLiked,
            @Value("${app.post.events.topics.post-like-removed:post.like.removed}") String topicPostLikeRemoved,
            @Value("${app.post.events.topics.comment-created:comment.created}") String topicCommentCreated,
            @Value("${app.post.events.topics.tag-created:tag.created}") String topicTagCreated) {
        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
        this.topicPostCreated = topicPostCreated;
        this.topicPostLiked = topicPostLiked;
        this.topicPostLikeRemoved = topicPostLikeRemoved;
        this.topicCommentCreated = topicCommentCreated;
        this.topicTagCreated = topicTagCreated;
        logger.info("PostEventPublisher initialized (enabled={}, kafka={})",
                enabled, kafkaTemplate != null);
    }

    public void publishPostCreated(PostCreatedEvent event) {
        send(topicPostCreated, event.postId(), event);
    }

    public void publishPostLiked(PostLikedEvent event) {
        String topic = "unliked".equals(event.action()) ? topicPostLikeRemoved : topicPostLiked;
        send(topic, event.postId(), event);
    }

    public void publishCommentCreated(CommentCreatedEvent event) {
        send(topicCommentCreated, event.postId(), event);
    }

    public void publishTagCreated(TagCreatedEvent event) {
        send(topicTagCreated, event.tag(), event);
    }

    private void send(String topic, String key, Object payload) {
        if (!enabled || kafkaTemplate == null) {
            logger.debug("flow=post_event_skipped topic={} key={} reason={}",
                    topic, key, !enabled ? "disabled" : "no_template");
            return;
        }
        try {
            kafkaTemplate.send(topic, key, payload);
            logger.debug("flow=post_event_published topic={} key={}", topic, key);
        } catch (Exception ex) {
            logger.warn("flow=post_event_publish_failed topic={} key={} err={}",
                    topic, key, ex.toString());
        }
    }
}
