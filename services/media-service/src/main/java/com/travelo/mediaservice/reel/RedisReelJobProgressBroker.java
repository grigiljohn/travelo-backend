package com.travelo.mediaservice.reel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Redis Pub/Sub broker. Any replica that writes a snapshot publishes to
 * {@code reel:job:{jobId}:events}; any replica streaming to a client has an
 * active {@link RedisMessageListenerContainer} subscription on that channel.
 * <p>
 * Messages are newline-free JSON matching {@link ReelJobProgressTracker.Snapshot}:
 * <pre>{@code {"stage":"FILTERING","percent":42,"message":"encode","updatedAt":"..."}}</pre>
 */
public class RedisReelJobProgressBroker implements ReelJobProgressBroker {

    private static final Logger log = LoggerFactory.getLogger(RedisReelJobProgressBroker.class);
    private static final String CHANNEL_PREFIX = "reel:job:";
    private static final String CHANNEL_SUFFIX = ":events";

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer container;
    private final ObjectMapper objectMapper;

    public RedisReelJobProgressBroker(StringRedisTemplate redisTemplate,
                                      RedisMessageListenerContainer container,
                                      ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.container = container;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String jobId, ReelJobProgressTracker.Snapshot snapshot) {
        if (jobId == null || jobId.isBlank() || snapshot == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(wire(snapshot));
            redisTemplate.convertAndSend(channel(jobId), payload);
        } catch (Exception e) {
            // Never propagate broker failures into the FFmpeg pipeline.
            log.warn("redis broker publish failed jobId={}: {}", jobId, e.toString());
        }
    }

    @Override
    public Subscription subscribe(String jobId, Consumer<ReelJobProgressTracker.Snapshot> listener) {
        if (jobId == null || jobId.isBlank() || listener == null) {
            return () -> { /* no-op */ };
        }
        ChannelTopic topic = new ChannelTopic(channel(jobId));
        MessageListener redisListener = (message, pattern) -> {
            try {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                @SuppressWarnings("rawtypes")
                Map raw = objectMapper.readValue(body, Map.class);
                ReelJobProgressTracker.Snapshot snap = fromWire(raw);
                if (snap != null) {
                    listener.accept(snap);
                }
            } catch (Exception e) {
                log.warn("redis broker listener decode failed jobId={}: {}", jobId, e.toString());
            }
        };
        container.addMessageListener(redisListener, topic);
        return () -> {
            try {
                container.removeMessageListener(redisListener, topic);
            } catch (Exception e) {
                log.debug("redis broker unsubscribe issue jobId={}: {}", jobId, e.toString());
            }
        };
    }

    private static String channel(String jobId) {
        return CHANNEL_PREFIX + jobId + CHANNEL_SUFFIX;
    }

    private static Map<String, Object> wire(ReelJobProgressTracker.Snapshot s) {
        Map<String, Object> m = new HashMap<>();
        m.put("stage", s.stage() == null ? null : s.stage().name());
        m.put("percent", s.percent());
        m.put("message", s.message());
        m.put("updatedAt", s.updatedAt() == null ? null : s.updatedAt().toString());
        return m;
    }

    @SuppressWarnings("rawtypes")
    static ReelJobProgressTracker.Snapshot fromWire(Map m) {
        if (m == null) return null;
        Object stageRaw = m.get("stage");
        if (stageRaw == null) return null;
        ReelJobStage stage;
        try {
            stage = ReelJobStage.valueOf(stageRaw.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
        Integer percent = null;
        Object p = m.get("percent");
        if (p instanceof Number n) {
            percent = n.intValue();
        } else if (p != null) {
            try { percent = Integer.parseInt(p.toString()); } catch (NumberFormatException ignored) {}
        }
        String message = m.get("message") == null ? null : m.get("message").toString();
        Instant updatedAt;
        try {
            Object t = m.get("updatedAt");
            updatedAt = t == null ? Instant.now() : Instant.parse(t.toString());
        } catch (Exception e) {
            updatedAt = Instant.now();
        }
        return new ReelJobProgressTracker.Snapshot(stage, message, percent, updatedAt);
    }
}
