package de.bcpeducation.jokes.punchline.domain;

import java.util.Objects;

public record Punchline(
        String id,
        String setupId,
        JokeCategory category,
        String text
) {

    public Punchline {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(setupId, "setupId must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(text, "text must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        if (setupId.isBlank()) {
            throw new IllegalArgumentException("setupId must not be blank");
        }

        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
    }
}
