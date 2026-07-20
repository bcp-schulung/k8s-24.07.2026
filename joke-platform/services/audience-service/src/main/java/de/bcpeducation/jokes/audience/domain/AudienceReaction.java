package de.bcpeducation.jokes.audience.domain;

public enum AudienceReaction {

    ROARING_LAUGHTER(
            "roaring-laughter",
            "The audience erupts into uncontrollable laughter!",
            10
    ),
    LAUGHTER(
            "laughter",
            "The audience laughs enthusiastically.",
            7
    ),
    POLITE_CHUCKLE(
            "polite-chuckle",
            "A few polite chuckles ripple through the room.",
            4
    ),
    AWKWARD_SILENCE(
            "awkward-silence",
            "An awkward silence fills the room.",
            0
    ),
    GROAN(
            "groan",
            "The audience groans in collective disappointment.",
            -2
    ),
    TOMATO(
            "tomato",
            "Someone throws a virtual tomato at the comedian.",
            -5
    );

    private final String value;
    private final String description;
    private final int baseScore;

    AudienceReaction(
            String value,
            String description,
            int baseScore
    ) {
        this.value = value;
        this.description = description;
        this.baseScore = baseScore;
    }

    public String value() {
        return value;
    }

    public String description() {
        return description;
    }

    public int baseScore() {
        return baseScore;
    }
}
