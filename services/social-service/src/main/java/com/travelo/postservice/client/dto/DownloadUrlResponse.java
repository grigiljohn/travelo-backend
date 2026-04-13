package com.travelo.postservice.client.dto;

public record DownloadUrlResponse(
        String url,
        Integer expiresIn
) {
}

