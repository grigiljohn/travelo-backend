package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record PollDto(
    @JsonProperty("id") String id,
    @JsonProperty("post_id") String postId,
    @JsonProperty("question") String question,
    @JsonProperty("options") List<String> options,
    @JsonProperty("total_votes") Integer totalVotes,
    @JsonProperty("vote_counts") Map<Integer, Long> voteCounts,
    @JsonProperty("expires_at") OffsetDateTime expiresAt,
    @JsonProperty("created_at") OffsetDateTime createdAt
) {}

