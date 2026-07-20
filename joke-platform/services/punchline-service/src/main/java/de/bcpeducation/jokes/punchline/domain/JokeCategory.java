package de.bcpeducation.jokes.punchline.domain;

import java.util.Arrays;
import java.util.Locale;

public enum JokeCategory {

    PROGRAMMING("programming"),
    KUBERNETES("kubernetes"),
    DAD("dad"),
    ANIMAL("animal"),
    SCIENCE("science");

    private final String value;

    JokeCategory(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static JokeCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "A joke category must be provided"
            );
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(category -> category.value.equals(normalized))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown joke category: " + value
                        )
                );
    }
}
