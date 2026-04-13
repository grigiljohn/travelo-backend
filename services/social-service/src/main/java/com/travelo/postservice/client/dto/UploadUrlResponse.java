package com.travelo.postservice.client.dto;

import java.util.List;
import java.util.UUID;

public record UploadUrlResponse(
        UUID mediaId,
        String uploadMethod,
        Integer expiresIn,
        String uploadUrl,
        String storageKey,
        String uploadId,
        Long partSize,
        List<String> presignedPartUrls
) {
}

