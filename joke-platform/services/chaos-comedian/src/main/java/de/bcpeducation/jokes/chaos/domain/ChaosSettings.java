package de.bcpeducation.jokes.chaos.domain;

public record ChaosSettings(
        int failurePercentage,
        int delayPercentage,
        int weirdPercentage,
        long minimumDelayMs,
        long maximumDelayMs
) {

    public ChaosSettings {
        validatePercentage(
                failurePercentage,
                "failurePercentage"
        );

        validatePercentage(
                delayPercentage,
                "delayPercentage"
        );

        validatePercentage(
                weirdPercentage,
                "weirdPercentage"
        );

        if (failurePercentage
                + delayPercentage
                + weirdPercentage > 100) {
            throw new IllegalArgumentException(
                    "The combined chaos percentages must not exceed 100"
            );
        }

        if (minimumDelayMs < 0) {
            throw new IllegalArgumentException(
                    "minimumDelayMs must not be negative"
            );
        }

        if (maximumDelayMs < minimumDelayMs) {
            throw new IllegalArgumentException(
                    "maximumDelayMs must be greater than or equal to minimumDelayMs"
            );
        }

        if (maximumDelayMs > 30_000) {
            throw new IllegalArgumentException(
                    "maximumDelayMs must not exceed 30000"
            );
        }
    }

    public static ChaosSettings defaults() {
        return new ChaosSettings(
                15,
                30,
                15,
                250,
                2_000
        );
    }

    public int normalPercentage() {
        return 100
                - failurePercentage
                - delayPercentage
                - weirdPercentage;
    }

    private static void validatePercentage(
            int value,
            String name
    ) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(
                    name + " must be between 0 and 100"
            );
        }
    }
}
