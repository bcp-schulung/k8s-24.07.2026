package de.bcpeducation.jokes.gateway.client;

public record AudienceReactionRequestDto(
        String setupId,
        String category,
        String setup,
        String punchline,
        Long seed
) {
}
