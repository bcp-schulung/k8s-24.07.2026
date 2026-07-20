package de.bcpeducation.jokes.gateway.client;

public record ChaosRequestDto(
        String mode,
        String message,
        Long seed
) {
}
