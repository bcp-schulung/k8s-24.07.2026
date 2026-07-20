package de.bcpeducation.jokes.gateway.service;

import de.bcpeducation.jokes.gateway.client.AudienceReactionDto;
import de.bcpeducation.jokes.gateway.client.AudienceReactionRequestDto;
import de.bcpeducation.jokes.gateway.client.ChaosRequestDto;
import de.bcpeducation.jokes.gateway.client.ChaosResponseDto;
import de.bcpeducation.jokes.gateway.client.JokePlatformClient;
import de.bcpeducation.jokes.gateway.client.JokeSetupDto;
import de.bcpeducation.jokes.gateway.client.PunchlineDto;
import de.bcpeducation.jokes.gateway.domain.CompleteJoke;
import de.bcpeducation.jokes.gateway.domain.ServiceTrace;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class JokeOrchestrationService {

    private final JokePlatformClient client;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    @Autowired
    public JokeOrchestrationService(
            JokePlatformClient client,
            MeterRegistry meterRegistry
    ) {
        this(
                client,
                meterRegistry,
                Clock.systemUTC()
        );
    }

    JokeOrchestrationService(
            JokePlatformClient client,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.client = client;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    public CompleteJoke createCompleteJoke(
            String category,
            String chaosMode,
            Long seed,
            String gatewayInstance
    ) {
        String normalizedChaosMode =
                normalizeChaosMode(chaosMode);

        JokeSetupDto setup =
                client.generateSetup(
                        normalizeCategory(category),
                        seed
                );

        PunchlineDto punchline =
                client.resolvePunchline(
                        setup.id()
                );

        CompleteJoke.ChaosResult chaosResult =
                invokeChaosIfRequested(
                        normalizedChaosMode,
                        setup,
                        punchline,
                        seed
                );

        AudienceReactionDto audience =
                client.createReaction(
                        new AudienceReactionRequestDto(
                                setup.id(),
                                setup.category(),
                                setup.text(),
                                punchline.text(),
                                seed
                        )
                );

        incrementCompletionCounter(
                setup.category(),
                normalizedChaosMode
        );

        return new CompleteJoke(
                UUID.randomUUID().toString(),
                setup.id(),
                setup.category(),
                setup.text(),
                punchline.text(),
                new CompleteJoke.AudienceResult(
                        audience.reactionId(),
                        audience.reaction(),
                        audience.description(),
                        audience.score(),
                        audience.statisticsRecorded(),
                        audience.eventPublished()
                ),
                chaosResult,
                new ServiceTrace(
                        gatewayInstance,
                        setup.generatedBy(),
                        punchline.resolvedBy(),
                        audience.reactedBy(),
                        chaosResult.handledBy()
                ),
                Instant.now(clock)
        );
    }

    private CompleteJoke.ChaosResult invokeChaosIfRequested(
            String chaosMode,
            JokeSetupDto setup,
            PunchlineDto punchline,
            Long seed
    ) {
        if ("none".equals(chaosMode)) {
            return CompleteJoke.ChaosResult.skipped();
        }

        ChaosResponseDto chaos =
                client.invokeChaos(
                        new ChaosRequestDto(
                                chaosMode,
                                setup.text()
                                        + " "
                                        + punchline.text(),
                                seed
                        )
                );

        return new CompleteJoke.ChaosResult(
                true,
                chaos.requestedMode(),
                chaos.appliedMode(),
                chaos.delayMs(),
                chaos.message(),
                chaos.oddity() == null
                        ? Map.of()
                        : chaos.oddity(),
                chaos.handledBy()
        );
    }

    private String normalizeCategory(
            String category
    ) {
        if (category == null || category.isBlank()) {
            return null;
        }

        return category
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeChaosMode(
            String chaosMode
    ) {
        if (chaosMode == null || chaosMode.isBlank()) {
            return "none";
        }

        return chaosMode
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private void incrementCompletionCounter(
            String category,
            String chaosMode
    ) {
        Counter.builder(
                        "joke_gateway_completed_jokes"
                )
                .description(
                        "Number of successfully completed jokes"
                )
                .tag("category", category)
                .tag("chaos_mode", chaosMode)
                .register(meterRegistry)
                .increment();
    }
}
