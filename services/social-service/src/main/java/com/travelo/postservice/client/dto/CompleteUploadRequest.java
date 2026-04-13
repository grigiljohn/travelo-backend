package com.travelo.postservice.client.dto;

public record CompleteUploadRequest(
        String etag,
        Long sizeBytes
) {
}

