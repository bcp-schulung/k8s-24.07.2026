package de.bcpeducation.jokes.gateway.domain;

public record ServiceTrace(
        String gateway,
        String jokeGenerator,
        String punchlineService,
        String audienceService,
        String chaosComedian
) {
}
