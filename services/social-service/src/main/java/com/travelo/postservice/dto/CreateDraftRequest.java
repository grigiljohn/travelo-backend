package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request DTO for creating a post draft.
 */
public record CreateDraftRequest(
    @JsonProperty("media_file_path")
    String mediaFilePath,

    @JsonProperty("is_video")
    Boolean isVideo,

    String title,

    String caption,

    String text,

    List<String> hashtags,

    String location,

    @JsonProperty("tagged_users")
    List<String> taggedUsers,

    String audience,

    @JsonProperty("allow_comments")
    Boolean allowComments,

    @JsonProperty("hide_likes_count")
    Boolean hideLikesCount,

    @JsonProperty("allow_remixing")
    Boolean allowRemixing,

    @JsonProperty("ai_label_enabled")
    Boolean aiLabelEnabled,

    @JsonProperty("music_track_id")
    String musicTrackId,

    String filter,

    @JsonProperty("create_mode")
    String createMode,

    @JsonProperty("cover_image_path")
    String coverImagePath,

    @JsonProperty("scheduled_at")
    java.time.OffsetDateTime scheduledAt
) {
}

