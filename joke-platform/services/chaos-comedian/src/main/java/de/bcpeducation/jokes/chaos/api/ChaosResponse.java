package de.bcpeducation.jokes.chaos.api;

import de.bcpeducation.jokes.chaos.domain.ChaosMode;

import java.time.Instant;
import java.util.Map;

public record ChaosResponse(
        String requestId,
        String requestedMode,
        String appliedMode,
        String message,
        long delayMs,
        Map<String, Object> oddity,
        String handledBy,
        Instant handledAt
) {

    public static ChaosResponse normal(
            String requestId,
            ChaosMode requestedMode,
            String message,
            String handledBy,
            Instant handledAt
    ) {
        return new ChaosResponse(
                requestId,
                requestedMode.value(),
                ChaosMode.NORMAL.value(),
                message,
                0,
                Map.of(),
                handledBy,
                handledAt
        );
    }

    public static ChaosResponse delayed(
            String requestId,
            ChaosMode requestedMode,
            String message,
            long delayMs,
            String handledBy,
            Instant handledAt
    ) {
        return new ChaosResponse(
                requestId,
                requestedMode.value(),
                ChaosMode.DELAY.value(),
                message,
                delayMs,
                Map.of(),
                handledBy,
                handledAt
        );
    }

    public static ChaosResponse weird(
            String requestId,
            ChaosMode requestedMode,
            String message,
            Map<String, Object> oddity,
            String handledBy,
            Instant handledAt
    ) {
        return new ChaosResponse(
                requestId,
                requestedMode.value(),
                ChaosMode.WEIRD.value(),
                message,
                0,
                oddity,
                handledBy,
                handledAt
        );
    }
}
