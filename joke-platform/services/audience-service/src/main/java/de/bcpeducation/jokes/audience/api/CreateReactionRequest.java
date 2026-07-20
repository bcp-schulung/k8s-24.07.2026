package de.bcpeducation.jokes.audience.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateReactionRequest(

        @NotBlank(message = "setupId must not be blank")
        @Size(
                max = 100,
                message = "setupId must contain at most 100 characters"
        )
        @Pattern(
                regexp = "[a-zA-Z0-9-]+",
                message = """
                        setupId may contain only letters, numbers \
                        and hyphens
                        """
        )
        String setupId,

        @NotBlank(message = "category must not be blank")
        @Pattern(
                regexp = "programming|kubernetes|dad|animal|science",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = """
                        category must be one of: programming, \
                        kubernetes, dad, animal, science
                        """
        )
        String category,

        @NotBlank(message = "setup must not be blank")
        @Size(
                max = 500,
                message = "setup must contain at most 500 characters"
        )
        String setup,

        @NotBlank(message = "punchline must not be blank")
        @Size(
                max = 500,
                message = "punchline must contain at most 500 characters"
        )
        String punchline,

        @Min(
                value = 0,
                message = "seed must be greater than or equal to zero"
        )
        Long seed
) {
}
