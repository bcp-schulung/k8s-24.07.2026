package de.bcpeducation.jokes.audience.api;

import de.bcpeducation.jokes.audience.domain.AudienceStatistics;

import java.util.Map;

public record AudienceStatisticsResponse(
        long totalReactions,
        long totalScore,
        double averageScore,
        Map<String, Long> reactions,
        String statisticsProvider
) {

    public static AudienceStatisticsResponse from(
            AudienceStatistics statistics,
            String statisticsProvider
    ) {
        return new AudienceStatisticsResponse(
                statistics.totalReactions(),
                statistics.totalScore(),
                statistics.averageScore(),
                statistics.reactions(),
                statisticsProvider
        );
    }
}
