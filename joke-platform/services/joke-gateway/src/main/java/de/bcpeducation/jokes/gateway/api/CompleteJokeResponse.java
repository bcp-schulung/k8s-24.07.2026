package de.bcpeducation.jokes.gateway.api;

import de.bcpeducation.jokes.gateway.domain.CompleteJoke;

import java.time.Instant;
import java.util.Map;

public record CompleteJokeResponse(
        String requestId,
        String setupId,
        String category,
        String setup,
        String punchline,
        AudienceResponse audience,
        ChaosResponse chaos,
        TraceResponse trace,
        Instant completedAt
) {

    public static CompleteJokeResponse from(
            CompleteJoke joke
    ) {
        return new CompleteJokeResponse(
                joke.requestId(),
                joke.setupId(),
                joke.category(),
                joke.setup(),
                joke.punchline(),
                AudienceResponse.from(
                        joke.audience()
                ),
                ChaosResponse.from(
                        joke.chaos()
                ),
                TraceResponse.from(
                        joke.trace()
                ),
                joke.completedAt()
        );
    }

    public record AudienceResponse(
            String reactionId,
            String reaction,
            String description,
            int score,
            boolean statisticsRecorded,
            boolean eventPublished
    ) {

        private static AudienceResponse from(
                CompleteJoke.AudienceResult result
        ) {
            return new AudienceResponse(
                    result.reactionId(),
                    result.reaction(),
                    result.description(),
                    result.score(),
                    result.statisticsRecorded(),
                    result.eventPublished()
            );
        }
    }

    public record ChaosResponse(
            boolean invoked,
            String requestedMode,
            String appliedMode,
            long delayMs,
            String message,
            Map<String, Object> oddity,
            String handledBy
    ) {

        private static ChaosResponse from(
                CompleteJoke.ChaosResult result
        ) {
            return new ChaosResponse(
                    result.invoked(),
                    result.requestedMode(),
                    result.appliedMode(),
                    result.delayMs(),
                    result.message(),
                    result.oddity(),
                    result.handledBy()
            );
        }
    }

    public record TraceResponse(
            String gateway,
            String jokeGenerator,
            String punchlineService,
            String audienceService,
            String chaosComedian
    ) {

        private static TraceResponse from(
                de.bcpeducation.jokes.gateway.domain.ServiceTrace trace
        ) {
            return new TraceResponse(
                    trace.gateway(),
                    trace.jokeGenerator(),
                    trace.punchlineService(),
                    trace.audienceService(),
                    trace.chaosComedian()
            );
        }
    }
}
