package de.bcpeducation.jokes.punchline.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResolvePunchlineRequest(

        @NotBlank(message = "setupId must not be blank")
        @Size(
                max = 100,
                message = "setupId must contain at most 100 characters"
        )
        @Pattern(
                regexp = "[a-zA-Z0-9-]+",
                message = "setupId may contain only letters, numbers and hyphens"
        )
        String setupId
) {
}
