package de.bcpeducation.jokes.chaos.error;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String requestId,
        String instance,
        List<FieldViolation> violations,
        Map<String, Object> details
) {

    public record FieldViolation(
            String field,
            String message
    ) {
    }
}
