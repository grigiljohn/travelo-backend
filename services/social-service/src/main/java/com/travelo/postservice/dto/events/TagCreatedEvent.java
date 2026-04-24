package com.travelo.postservice.dto.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Emitted on `tag.created` Kafka topic when a previously unknown tag is used.
 * Consumers: discovery-service (trending tag index).
 */
public record TagCreatedEvent(
        @JsonProperty("tag")        String tag,
        @JsonProperty("first_post") String firstPostId,
        @JsonProperty("author_id")  String authorId,
        @JsonProperty("created_at") Instant createdAt
) {}
