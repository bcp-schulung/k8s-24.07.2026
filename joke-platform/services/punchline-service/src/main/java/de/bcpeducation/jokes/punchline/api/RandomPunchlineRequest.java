package de.bcpeducation.jokes.punchline.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record RandomPunchlineRequest(

        @Pattern(
                regexp = "programming|kubernetes|dad|animal|science",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = """
                        category must be one of: programming, kubernetes, \
                        dad, animal, science
                        """
        )
        String category,

        @Min(
                value = 0,
                message = "seed must be greater than or equal to zero"
        )
        Long seed
) {
}
