package de.bcpeducation.jokes.gateway.client;

public record PunchlineDto(
        String id,
        String setupId,
        String category,
        String text,
        String resolvedBy
) {
}
