package de.bcpeducation.jokes.chaos.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateChaosSettingsRequest(

        @Min(
                value = 0,
                message = "failurePercentage must be at least 0"
        )
        @Max(
                value = 100,
                message = "failurePercentage must not exceed 100"
        )
        Integer failurePercentage,

        @Min(
                value = 0,
                message = "delayPercentage must be at least 0"
        )
        @Max(
                value = 100,
                message = "delayPercentage must not exceed 100"
        )
        Integer delayPercentage,

        @Min(
                value = 0,
                message = "weirdPercentage must be at least 0"
        )
        @Max(
                value = 100,
                message = "weirdPercentage must not exceed 100"
        )
        Integer weirdPercentage,

        @Min(
                value = 0,
                message = "minimumDelayMs must not be negative"
        )
        Long minimumDelayMs,

        @Min(
                value = 0,
                message = "maximumDelayMs must not be negative"
        )
        @Max(
                value = 30000,
                message = "maximumDelayMs must not exceed 30000"
        )
        Long maximumDelayMs
) {
}
