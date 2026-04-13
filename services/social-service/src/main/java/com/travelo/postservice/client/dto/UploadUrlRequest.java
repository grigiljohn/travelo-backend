package com.travelo.postservice.client.dto;

import java.util.List;
import java.util.UUID;

public record UploadUrlRequest(
        UUID ownerId,
        String filename,
        String mimeType,
        Long sizeBytes,
        String mediaType,  // IMAGE, VIDEO, AUDIO, OTHER
        Boolean resumable,
        List<String> tags
) {
}

