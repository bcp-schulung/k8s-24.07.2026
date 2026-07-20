package de.bcpeducation.jokes.audience.domain;

import java.util.Map;

public record AudienceStatistics(
        long totalReactions,
        long totalScore,
        double averageScore,
        Map<String, Long> reactions
) {

    public AudienceStatistics {
        reactions = Map.copyOf(reactions);
    }

    public static AudienceStatistics empty() {
        return new AudienceStatistics(
                0,
                0,
                0.0,
                Map.of()
        );
    }
}
