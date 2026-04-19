package com.travelo.momentsservice.model;

import com.travelo.momentsservice.dto.MomentCreateResponse;

import java.time.OffsetDateTime;

public record MomentIdempotencyRecord(
        String key,
        String userId,
        MomentCreateResponse response,
        OffsetDateTime createdAt
) {
}

