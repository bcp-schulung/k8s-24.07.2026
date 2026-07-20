package de.bcpeducation.jokes.punchline.api;

import de.bcpeducation.jokes.punchline.domain.Punchline;

public record PunchlineResponse(
        String id,
        String setupId,
        String category,
        String text,
        String resolvedBy
) {

    public static PunchlineResponse from(
            Punchline punchline,
            String resolvedBy
    ) {
        return new PunchlineResponse(
                punchline.id(),
                punchline.setupId(),
                punchline.category().value(),
                punchline.text(),
                resolvedBy
        );
    }
}
