package de.bcpeducation.jokes.chaos.service;

import de.bcpeducation.jokes.chaos.api.ChaosResponse;
import de.bcpeducation.jokes.chaos.domain.ChaosMode;
import de.bcpeducation.jokes.chaos.domain.ChaosSettings;
import de.bcpeducation.jokes.chaos.error.SimulatedChaosException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;
import java.util.random.RandomGenerator;

@Service
public class ChaosComedianService {

    private static final List<String> WEIRD_MESSAGES =
            List.of(
                    "The punchline has been rescheduled onto another node.",
                    "This joke is eventually consistent.",
                    "The comedian returned HTTP 200 but emotionally feels like a 503.",
                    "A sidecar whispered the punchline before the comedian could.",
                    "The audience has entered CrashLoopBackOff.",
                    "The joke scaled horizontally and is now twice as confusing."
            );

    private final ChaosSettingsService settingsService;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    @Autowired
    public ChaosComedianService(
            ChaosSettingsService settingsService,
            MeterRegistry meterRegistry
    ) {
        this(
                settingsService,
                meterRegistry,
                Clock.systemUTC()
        );
    }

    ChaosComedianService(
            ChaosSettingsService settingsService,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.settingsService = settingsService;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    public ChaosResponse perform(
            String requestedModeValue,
            String message,
            Long seed,
            String instanceName
    ) {
        ChaosMode requestedMode =
                ChaosMode.fromValue(requestedModeValue);

        RandomGenerator randomGenerator =
                createRandomGenerator(seed);

        ChaosMode appliedMode =
                requestedMode == ChaosMode.RANDOM
                        ? selectRandomMode(
                                settingsService.getSettings(),
                                randomGenerator
                        )
                        : requestedMode;

        String requestId =
                UUID.randomUUID().toString();

        String effectiveMessage =
                message == null || message.isBlank()
                        ? "The comedian approaches the microphone."
                        : message.trim();

        return switch (appliedMode) {
            case NORMAL -> createNormalResponse(
                    requestId,
                    requestedMode,
                    effectiveMessage,
                    instanceName
            );

            case DELAY -> createDelayedResponse(
                    requestId,
                    requestedMode,
                    effectiveMessage,
                    instanceName,
                    randomGenerator
            );

            case WEIRD -> createWeirdResponse(
                    requestId,
                    requestedMode,
                    effectiveMessage,
                    instanceName,
                    randomGenerator
            );

            case ERROR -> throwFailure(
                    requestId,
                    instanceName
            );

            case RANDOM -> throw new IllegalStateException(
                    "RANDOM must be resolved before execution"
            );
        };
    }

    private ChaosResponse createNormalResponse(
            String requestId,
            ChaosMode requestedMode,
            String message,
            String instanceName
    ) {
        incrementCounter(
                ChaosMode.NORMAL,
                instanceName
        );

        return ChaosResponse.normal(
                requestId,
                requestedMode,
                message + " Everything works. Suspiciously.",
                instanceName,
                Instant.now(clock)
        );
    }

    private ChaosResponse createDelayedResponse(
            String requestId,
            ChaosMode requestedMode,
            String message,
            String instanceName,
            RandomGenerator randomGenerator
    ) {
        long delayMs =
                selectDelay(
                        settingsService.getSettings(),
                        randomGenerator
                );

        sleep(delayMs);

        incrementCounter(
                ChaosMode.DELAY,
                instanceName
        );

        return ChaosResponse.delayed(
                requestId,
                requestedMode,
                message + " The punchline arrived eventually.",
                delayMs,
                instanceName,
                Instant.now(clock)
        );
    }

    private ChaosResponse createWeirdResponse(
            String requestId,
            ChaosMode requestedMode,
            String message,
            String instanceName,
            RandomGenerator randomGenerator
    ) {
        String weirdMessage =
                WEIRD_MESSAGES.get(
                        randomGenerator.nextInt(
                                WEIRD_MESSAGES.size()
                        )
                );

        Map<String, Object> oddity = Map.of(
                "unexpectedMessage",
                weirdMessage,
                "bananaCount",
                randomGenerator.nextInt(1, 100),
                "isThisFine",
                randomGenerator.nextBoolean(),
                "recommendedReplicaCount",
                randomGenerator.nextInt(2, 12)
        );

        incrementCounter(
                ChaosMode.WEIRD,
                instanceName
        );

        return ChaosResponse.weird(
                requestId,
                requestedMode,
                message,
                oddity,
                instanceName,
                Instant.now(clock)
        );
    }

    private ChaosResponse throwFailure(
            String requestId,
            String instanceName
    ) {
        incrementCounter(
                ChaosMode.ERROR,
                instanceName
        );

        throw new SimulatedChaosException(
                requestId,
                instanceName
        );
    }

    private ChaosMode selectRandomMode(
            ChaosSettings settings,
            RandomGenerator randomGenerator
    ) {
        int roll =
                randomGenerator.nextInt(100);

        int errorBoundary =
                settings.failurePercentage();

        int delayBoundary =
                errorBoundary
                + settings.delayPercentage();

        int weirdBoundary =
                delayBoundary
                + settings.weirdPercentage();

        if (roll < errorBoundary) {
            return ChaosMode.ERROR;
        }

        if (roll < delayBoundary) {
            return ChaosMode.DELAY;
        }

        if (roll < weirdBoundary) {
            return ChaosMode.WEIRD;
        }

        return ChaosMode.NORMAL;
    }

    private long selectDelay(
            ChaosSettings settings,
            RandomGenerator randomGenerator
    ) {
        long minimum =
                settings.minimumDelayMs();

        long maximum =
                settings.maximumDelayMs();

        if (minimum == maximum) {
            return minimum;
        }

        return randomGenerator.nextLong(
                minimum,
                maximum + 1
        );
    }

    private RandomGenerator createRandomGenerator(
            Long seed
    ) {
        if (seed == null) {
            return new Random();
        }

        return new Random(seed);
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "The delayed joke was interrupted",
                    exception
            );
        }
    }

    private void incrementCounter(
            ChaosMode mode,
            String instanceName
    ) {
        Counter.builder("chaos_comedian_requests")
                .description(
                        "Number of chaos comedian requests"
                )
                .tag("mode", mode.value())
                .tag("instance", instanceName)
                .register(meterRegistry)
                .increment();
    }
}
