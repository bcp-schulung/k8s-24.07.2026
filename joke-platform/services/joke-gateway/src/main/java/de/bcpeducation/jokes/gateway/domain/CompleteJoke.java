package de.bcpeducation.jokes.gateway.domain;

import java.time.Instant;
import java.util.Map;

public record CompleteJoke(
        String requestId,
        String setupId,
        String category,
        String setup,
        String punchline,
        AudienceResult audience,
        ChaosResult chaos,
        ServiceTrace trace,
        Instant completedAt
) {

    public record AudienceResult(
            String reactionId,
            String reaction,
            String description,
            int score,
            boolean statisticsRecorded,
            boolean eventPublished
    ) {
    }

    public record ChaosResult(
            boolean invoked,
            String requestedMode,
            String appliedMode,
            long delayMs,
            String message,
            Map<String, Object> oddity,
            String handledBy
    ) {

        public static ChaosResult skipped() {
            return new ChaosResult(
                    false,
                    "none",
                    "none",
                    0,
                    "Chaos was not invoked",
                    Map.of(),
                    "not-invoked"
            );
        }
    }
}
