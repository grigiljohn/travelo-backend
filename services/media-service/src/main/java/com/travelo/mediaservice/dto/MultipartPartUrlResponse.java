package com.travelo.mediaservice.dto;

import java.util.List;

/**
 * Response for presigned multipart part URLs.
 */
public record MultipartPartUrlResponse(
        List<String> partUrls  // presigned URLs for each part (1-indexed)
) {
}

