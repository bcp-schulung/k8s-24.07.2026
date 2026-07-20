package de.bcpeducation.jokes.chaos.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TerminationRequest(

        @Min(
                value = 100,
                message = "delayMs must be at least 100"
        )
        @Max(
                value = 10000,
                message = "delayMs must not exceed 10000"
        )
        Long delayMs,

        @Size(
                max = 200,
                message = "reason must contain at most 200 characters"
        )
        String reason
) {
}
