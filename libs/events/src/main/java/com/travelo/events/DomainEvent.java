package com.travelo.events;

import java.time.Instant;
import java.util.UUID;

public record DomainEvent(
        UUID id,
        String type,
        Instant occurredAt,
        Object payload
) {
    public static DomainEvent of(String type, Object payload) {
        return new DomainEvent(UUID.randomUUID(), type, Instant.now(), payload);
    }
}
