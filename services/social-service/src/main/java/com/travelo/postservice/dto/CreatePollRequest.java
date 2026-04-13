package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record CreatePollRequest(
    @JsonProperty("post_id") String postId,
    @NotBlank(message = "Question is required")
    @JsonProperty("question") String question,
    @NotNull(message = "Options are required")
    @Size(min = 2, max = 4, message = "Poll must have between 2 and 4 options")
    @JsonProperty("options") List<String> options,
    @JsonProperty("expires_at") OffsetDateTime expiresAt
) {}

