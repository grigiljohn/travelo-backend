package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for presigned upload URL request.
 */
public record RequestUploadUrlResponse(
    @JsonProperty("media_id")
    UUID mediaId,

    @JsonProperty("upload_method")
    String uploadMethod,

    @JsonProperty("expires_in")
    Integer expiresIn,

    @JsonProperty("upload_url")
    String uploadUrl,

    @JsonProperty("storage_key")
    String storageKey,

    @JsonProperty("upload_id")
    String uploadId,

    @JsonProperty("part_size")
    Long partSize,

    @JsonProperty("presigned_part_urls")
    List<String> presignedPartUrls
) {
}

