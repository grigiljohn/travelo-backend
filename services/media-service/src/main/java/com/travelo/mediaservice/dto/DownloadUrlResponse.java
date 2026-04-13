package com.travelo.mediaservice.dto;

/**
 * Response for GET /v1/media/{media_id}/download
 */
public record DownloadUrlResponse(
        String url,
        Integer expiresIn  // seconds
) {
}

