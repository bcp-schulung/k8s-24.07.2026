package de.bcpeducation.jokes.gateway.client;

public record JokeSetupDto(
        String id,
        String category,
        String text,
        String generatedBy
) {
}
