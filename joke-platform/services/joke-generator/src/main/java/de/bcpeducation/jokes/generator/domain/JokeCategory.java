package de.bcpeducation.jokes.generator.domain;

import java.util.Arrays;
import java.util.Locale;

public enum JokeCategory {

    PROGRAMMING("programming", "Programming"),
    KUBERNETES("kubernetes", "Kubernetes"),
    DAD("dad", "Dad jokes"),
    ANIMAL("animal", "Animals"),
    SCIENCE("science", "Science");

    private final String value;
    private final String displayName;

    JokeCategory(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String value() {
        return value;
    }

    public String displayName() {
        return displayName;
    }

    public static JokeCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A joke category must be provided");
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(category -> category.value.equals(normalizedValue))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown joke category: " + value
                        )
                );
    }
}
