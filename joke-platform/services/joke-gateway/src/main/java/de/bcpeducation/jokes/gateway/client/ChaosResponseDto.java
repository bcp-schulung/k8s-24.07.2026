package de.bcpeducation.jokes.gateway.client;

import java.time.Instant;
import java.util.Map;

public record ChaosResponseDto(
        String requestId,
        String requestedMode,
        String appliedMode,
        String message,
        long delayMs,
        Map<String, Object> oddity,
        String handledBy,
        Instant handledAt
) {
}
