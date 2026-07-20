package de.bcpeducation.jokes.chaos.api;

import java.time.Instant;

public record TerminationResponse(
        String message,
        String instance,
        long terminationDelayMs,
        String reason,
        Instant requestedAt
) {
}
