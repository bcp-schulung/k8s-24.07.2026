package de.bcpeducation.jokes.gateway.client;

import java.util.Map;

public record AudienceStatisticsDto(
        long totalReactions,
        long totalScore,
        double averageScore,
        Map<String, Long> reactions,
        String statisticsProvider
) {
}
