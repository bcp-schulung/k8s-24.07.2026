package de.bcpeducation.jokes.chaos.domain;

import java.util.Arrays;
import java.util.Locale;

public enum ChaosMode {

    RANDOM("random"),
    NORMAL("normal"),
    DELAY("delay"),
    ERROR("error"),
    WEIRD("weird");

    private final String value;

    ChaosMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ChaosMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return RANDOM;
        }

        String normalized =
                value.trim().toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(mode ->
                        mode.value.equals(normalized)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown chaos mode: " + value
                        )
                );
    }
}
