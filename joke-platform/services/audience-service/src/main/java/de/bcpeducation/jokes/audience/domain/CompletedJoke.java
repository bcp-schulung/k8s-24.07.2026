package de.bcpeducation.jokes.audience.domain;

import java.util.Objects;

public record CompletedJoke(
        String setupId,
        String category,
        String setup,
        String punchline
) {

    public CompletedJoke {
        Objects.requireNonNull(
                setupId,
                "setupId must not be null"
        );
        Objects.requireNonNull(
                category,
                "category must not be null"
        );
        Objects.requireNonNull(
                setup,
                "setup must not be null"
        );
        Objects.requireNonNull(
                punchline,
                "punchline must not be null"
        );

        if (setupId.isBlank()) {
            throw new IllegalArgumentException(
                    "setupId must not be blank"
            );
        }

        if (category.isBlank()) {
            throw new IllegalArgumentException(
                    "category must not be blank"
            );
        }

        if (setup.isBlank()) {
            throw new IllegalArgumentException(
                    "setup must not be blank"
            );
        }

        if (punchline.isBlank()) {
            throw new IllegalArgumentException(
                    "punchline must not be blank"
            );
        }
    }
}
