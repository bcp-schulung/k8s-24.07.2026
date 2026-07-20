package de.bcpeducation.jokes.audience.event;

import de.bcpeducation.jokes.audience.domain.ReactionResult;

import java.time.Instant;

public record AudienceReactionEvent(
        String eventType,
        String reactionId,
        String setupId,
        String category,
        String reaction,
        int score,
        Instant occurredAt,
        String sourceInstance
) {

    public static AudienceReactionEvent from(
            ReactionResult result,
            String sourceInstance
    ) {
        return new AudienceReactionEvent(
                "audience.reaction.created",
                result.reactionId(),
                result.setupId(),
                result.category(),
                result.reaction().value(),
                result.score(),
                result.reactedAt(),
                sourceInstance
        );
    }
}
