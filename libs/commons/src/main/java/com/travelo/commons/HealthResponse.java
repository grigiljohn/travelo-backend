package com.travelo.commons;

import java.time.OffsetDateTime;
import java.util.Map;

public final class HealthResponse {

    private HealthResponse() {
    }

    public static Map<String, Object> ok(String serviceName) {
        return Map.of(
                "service", serviceName,
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()
        );
    }
}
