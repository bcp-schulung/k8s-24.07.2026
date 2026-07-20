package de.bcpeducation.jokes.chaos.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChaosRequest(

        @Pattern(
                regexp = "random|normal|delay|error|weird",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = """
                        mode must be one of: random, normal, \
                        delay, error, weird
                        """
        )
        String mode,

        @Size(
                max = 500,
                message = "message must contain at most 500 characters"
        )
        String message,

        @Min(
                value = 0,
                message = "seed must be greater than or equal to zero"
        )
        Long seed
) {
}
