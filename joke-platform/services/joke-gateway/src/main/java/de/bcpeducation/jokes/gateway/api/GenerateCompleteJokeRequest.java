package de.bcpeducation.jokes.gateway.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record GenerateCompleteJokeRequest(

        @Pattern(
                regexp = "programming|kubernetes|dad|animal|science",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = """
                        category must be one of: programming, \
                        kubernetes, dad, animal, science
                        """
        )
        String category,

        @Pattern(
                regexp = "none|random|normal|delay|error|weird",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = """
                        chaosMode must be one of: none, random, \
                        normal, delay, error, weird
                        """
        )
        String chaosMode,

        @Min(
                value = 0,
                message = "seed must be greater than or equal to zero"
        )
        Long seed
) {
}
