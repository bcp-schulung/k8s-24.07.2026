package de.bcpeducation.jokes.audience.domain;

import java.time.Instant;
import java.util.Objects;

public record ReactionResult(
        String reactionId,
        String setupId,
        String category,
        AudienceReaction reaction,
        int score,
        Instant reactedAt
) {

    public ReactionResult {
        Objects.requireNonNull(
                reactionId,
                "reactionId must not be null"
        );
        Objects.requireNonNull(
                setupId,
                "setupId must not be null"
        );
        Objects.requireNonNull(
                category,
                "category must not be null"
        );
        Objects.requireNonNull(
                reaction,
                "reaction must not be null"
        );
        Objects.requireNonNull(
                reactedAt,
                "reactedAt must not be null"
        );
    }
}
