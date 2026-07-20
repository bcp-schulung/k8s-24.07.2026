package de.bcpeducation.jokes.generator.api;

import de.bcpeducation.jokes.generator.domain.JokeSetup;

public record JokeSetupResponse(
        String id,
        String category,
        String text,
        String generatedBy
) {

    public static JokeSetupResponse from(
            JokeSetup jokeSetup,
            String generatedBy
    ) {
        return new JokeSetupResponse(
                jokeSetup.id(),
                jokeSetup.category().value(),
                jokeSetup.text(),
                generatedBy
        );
    }
}
