package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
    @JsonProperty("post_type")
    @NotBlank(message = "Post type is required")
    @Pattern(regexp = "image|video|reel|text|mixed", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Post type must be one of: image, video, reel, text, mixed")
    String postType,

    // Deprecated: Use mediaItems instead
    String content,

    // Deprecated: Use mediaItems instead
    List<String> images,

    // New: Mixed media items (photos and videos). Optional for text posts (use content instead).
    @JsonProperty("media_items")
    @Valid
    @Size(max = 100)
    List<MediaItemRequest> mediaItems,

    String caption,

    List<String> tags,

    @NotBlank(message = "Mood is required")
    @Pattern(regexp = "chill|love|adventure|party|nature|food|culture|romantic|activity|relax|neutral|happy|excited|calm|inspired|grateful|thoughtful|motivated",
             flags = Pattern.Flag.CASE_INSENSITIVE, message = "Mood must be a valid mood type")
    String mood,

    String location,

    @JsonProperty("music_track")
    String musicTrack,

    @JsonProperty("visibility")
    String visibility
) {
}

