package de.bcpeducation.jokes.gateway.client;

import java.time.Instant;

public record AudienceReactionDto(
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
}
