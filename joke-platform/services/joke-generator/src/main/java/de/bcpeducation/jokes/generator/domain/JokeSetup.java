package de.bcpeducation.jokes.generator.domain;

import java.util.Objects;

public record JokeSetup(
        String id,
        JokeCategory category,
        String text
) {

    public JokeSetup {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(text, "text must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
    }
}
