package de.bcpeducation.jokes.chaos.api;

import de.bcpeducation.jokes.chaos.domain.ChaosSettings;

public record ChaosSettingsResponse(
        int failurePercentage,
        int delayPercentage,
        int weirdPercentage,
        int normalPercentage,
        long minimumDelayMs,
        long maximumDelayMs
) {

    public static ChaosSettingsResponse from(
            ChaosSettings settings
    ) {
        return new ChaosSettingsResponse(
                settings.failurePercentage(),
                settings.delayPercentage(),
                settings.weirdPercentage(),
                settings.normalPercentage(),
                settings.minimumDelayMs(),
                settings.maximumDelayMs()
        );
    }
}
