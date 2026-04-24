package com.travelo.postservice.dto.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Emitted on `post.created` Kafka topic after a new post is persisted.
 * Consumers: discovery-service (ES indexing), feed-service (follower fan-out).
 */
public record PostCreatedEvent(
        @JsonProperty("post_id")    String postId,
        @JsonProperty("author_id")  String authorId,
        @JsonProperty("post_type")  String postType,
        @JsonProperty("visibility") String visibility,
        @JsonProperty("caption")    String caption,
        @JsonProperty("tags")       List<String> tags,
        @JsonProperty("created_at") Instant createdAt
) {}
