package de.bcpeducation.jokes.audience.service;

import de.bcpeducation.jokes.audience.domain.AudienceReaction;
import de.bcpeducation.jokes.audience.domain.CompletedJoke;
import de.bcpeducation.jokes.audience.domain.ReactionResult;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.Random;
import java.util.random.RandomGenerator;

@Service
public class AudienceReactionService {

    private final Clock clock;

    public AudienceReactionService() {
        this(Clock.systemUTC());
    }

    AudienceReactionService(Clock clock) {
        this.clock = clock;
    }

    public ReactionResult react(
            CompletedJoke joke,
            Long seed
    ) {
        RandomGenerator randomGenerator =
                createRandomGenerator(
                        joke,
                        seed
                );

        int reactionRoll =
                calculateReactionRoll(
                        joke,
                        randomGenerator
                );

        AudienceReaction reaction =
                selectReaction(reactionRoll);

        int scoreVariation =
                randomGenerator.nextInt(-1, 2);

        int score = clamp(
                reaction.baseScore() + scoreVariation,
                -5,
                10
        );

        return new ReactionResult(
                UUID.randomUUID().toString(),
                joke.setupId(),
                joke.category()
                        .toLowerCase(Locale.ROOT),
                reaction,
                score,
                Instant.now(clock)
        );
    }

    private int calculateReactionRoll(
            CompletedJoke joke,
            RandomGenerator randomGenerator
    ) {
        int randomPart =
                randomGenerator.nextInt(100);

        int textPart =
                Math.floorMod(
                        (
                                joke.setup() +
                                joke.punchline()
                        ).hashCode(),
                        15
                ) - 7;

        int categoryBonus =
                switch (
                        joke.category()
                                .toLowerCase(Locale.ROOT)
                ) {
                    case "kubernetes" -> 8;
                    case "programming" -> 5;
                    case "dad" -> 2;
                    default -> 0;
                };

        return clamp(
                randomPart + textPart + categoryBonus,
                0,
                100
        );
    }

    private AudienceReaction selectReaction(int roll) {
        if (roll >= 92) {
            return AudienceReaction.ROARING_LAUGHTER;
        }

        if (roll >= 72) {
            return AudienceReaction.LAUGHTER;
        }

        if (roll >= 48) {
            return AudienceReaction.POLITE_CHUCKLE;
        }

        if (roll >= 28) {
            return AudienceReaction.AWKWARD_SILENCE;
        }

        if (roll >= 10) {
            return AudienceReaction.GROAN;
        }

        return AudienceReaction.TOMATO;
    }

    private RandomGenerator createRandomGenerator(
            CompletedJoke joke,
            Long seed
    ) {
        long effectiveSeed = seed != null
                ? seed
                : System.nanoTime()
                  ^ joke.setupId().hashCode();

        return new Random(effectiveSeed);
    }

    private int clamp(
            int value,
            int minimum,
            int maximum
    ) {
        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }
}
