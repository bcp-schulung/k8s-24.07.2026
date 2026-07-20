package de.bcpeducation.jokes.audience.api;

import de.bcpeducation.jokes.audience.domain.ReactionResult;

import java.time.Instant;

public record ReactionResponse(
        String reactionId,
        String setupId,
        String category,
        String reaction,
        String description,
        int score,
        Instant reactedAt,
        String reactedBy,
        boolean statisticsRecorded,
        boolean eventPublished
) {

    public static ReactionResponse from(
            ReactionResult result,
            String reactedBy,
            boolean statisticsRecorded,
            boolean eventPublished
    ) {
        return new ReactionResponse(
                result.reactionId(),
                result.setupId(),
                result.category(),
                result.reaction().value(),
                result.reaction().description(),
                result.score(),
                result.reactedAt(),
                reactedBy,
                statisticsRecorded,
                eventPublished
        );
    }
}
