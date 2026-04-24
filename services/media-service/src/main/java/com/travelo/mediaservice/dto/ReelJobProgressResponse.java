package com.travelo.mediaservice.dto;

import java.time.Instant;

/**
 * Status snapshot for a reel pipeline job. Clients poll {@code GET /v1/media/reel/jobs/{jobId}}
 * until {@code stage == READY} or {@code stage == FAILED}.
 */
public record ReelJobProgressResponse(
        String jobId,
        String stage,
        int percent,
        String message,
        Instant updatedAt
) {}
