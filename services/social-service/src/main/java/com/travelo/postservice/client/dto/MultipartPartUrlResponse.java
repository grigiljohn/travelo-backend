package com.travelo.postservice.client.dto;

import java.util.List;

public record MultipartPartUrlResponse(
        List<String> partUrls
) {
}

