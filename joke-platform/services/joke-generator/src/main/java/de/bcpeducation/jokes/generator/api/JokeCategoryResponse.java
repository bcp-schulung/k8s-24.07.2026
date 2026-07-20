package de.bcpeducation.jokes.generator.api;

import de.bcpeducation.jokes.generator.domain.JokeCategory;

public record JokeCategoryResponse(
        String value,
        String displayName
) {

    public static JokeCategoryResponse from(JokeCategory category) {
        return new JokeCategoryResponse(
                category.value(),
                category.displayName()
        );
    }
}
