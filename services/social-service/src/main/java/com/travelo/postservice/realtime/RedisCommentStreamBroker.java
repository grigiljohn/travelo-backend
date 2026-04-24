package com.travelo.postservice.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Redis Pub/Sub broker. Any replica that persists a comment publishes a JSON
 * frame to {@code post:{postId}:comments:events}; any replica streaming to a
 * client subscribes to that channel via {@link RedisMessageListenerContainer}.
 *
 * <p>On-wire shape matches {@link CommentStreamEvent} 1:1 (Jackson handles
 * serialization) so the browser/mobile client can decode each SSE frame the
 * same way it decodes a single element of {@code GET .../comments}.
 */
public class RedisCommentStreamBroker implements CommentStreamBroker {

    private static final Logger log = LoggerFactory.getLogger(RedisCommentStreamBroker.class);
    private static final String CHANNEL_PREFIX = "post:";
    private static final String CHANNEL_SUFFIX = ":comments:events";

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer container;
    private final ObjectMapper objectMapper;

    public RedisCommentStreamBroker(StringRedisTemplate redisTemplate,
                                    RedisMessageListenerContainer container,
                                    ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.container = container;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String postId, CommentStreamEvent event) {
        if (postId == null || postId.isBlank() || event == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel(postId), payload);
        } catch (Exception e) {
            // Never propagate broker failures into the comment write path.
            log.warn("redis comment broker publish failed postId={}: {}", postId, e.toString());
        }
    }

    @Override
    public Subscription subscribe(String postId, Consumer<CommentStreamEvent> listener) {
        if (postId == null || postId.isBlank() || listener == null) {
            return () -> { /* no-op */ };
        }
        ChannelTopic topic = new ChannelTopic(channel(postId));
        MessageListener redisListener = (message, pattern) -> {
            try {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                CommentStreamEvent ev = objectMapper.readValue(body, CommentStreamEvent.class);
                if (ev != null) {
                    listener.accept(ev);
                }
            } catch (Exception e) {
                log.warn("redis comment broker listener decode failed postId={}: {}",
                        postId, e.toString());
            }
        };
        container.addMessageListener(redisListener, topic);
        return () -> {
            try {
                container.removeMessageListener(redisListener, topic);
            } catch (Exception e) {
                log.debug("redis comment broker unsubscribe issue postId={}: {}", postId, e.toString());
            }
        };
    }

    private static String channel(String postId) {
        return CHANNEL_PREFIX + postId + CHANNEL_SUFFIX;
    }
}
