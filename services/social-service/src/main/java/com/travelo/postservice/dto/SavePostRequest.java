package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for saving/unsaving a post.
 */
public record SavePostRequest(
    @JsonProperty("collection_name")
    String collectionName
) {
}

