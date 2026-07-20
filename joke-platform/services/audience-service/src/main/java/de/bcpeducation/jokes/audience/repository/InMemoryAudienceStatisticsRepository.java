package de.bcpeducation.jokes.audience.repository;

import de.bcpeducation.jokes.audience.domain.AudienceReaction;
import de.bcpeducation.jokes.audience.domain.AudienceStatistics;
import de.bcpeducation.jokes.audience.domain.ReactionResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

@Repository
@ConditionalOnProperty(
        name = "audience.statistics.provider",
        havingValue = "memory",
        matchIfMissing = true
)
public class InMemoryAudienceStatisticsRepository
        implements AudienceStatisticsRepository {

    private final LongAdder totalReactions = new LongAdder();
    private final LongAdder totalScore = new LongAdder();

    private final Map<AudienceReaction, LongAdder>
            reactionCounters = createReactionCounters();

    @Override
    public void record(ReactionResult reactionResult) {
        totalReactions.increment();
        totalScore.add(reactionResult.score());

        reactionCounters
                .get(reactionResult.reaction())
                .increment();
    }

    @Override
    public AudienceStatistics getStatistics() {
        long reactionCount = totalReactions.sum();
        long score = totalScore.sum();

        double averageScore = reactionCount == 0
                ? 0.0
                : (double) score / reactionCount;

        Map<String, Long> reactions =
                new LinkedHashMap<>();

        reactionCounters.forEach(
                (reaction, counter) ->
                        reactions.put(
                                reaction.value(),
                                counter.sum()
                        )
        );

        return new AudienceStatistics(
                reactionCount,
                score,
                averageScore,
                reactions
        );
    }

    @Override
    public void reset() {
        totalReactions.reset();
        totalScore.reset();

        reactionCounters
                .values()
                .forEach(LongAdder::reset);
    }

    @Override
    public String providerName() {
        return "memory";
    }

    private Map<AudienceReaction, LongAdder>
            createReactionCounters() {

        Map<AudienceReaction, LongAdder> counters =
                new LinkedHashMap<>();

        Arrays.stream(AudienceReaction.values())
                .forEach(reaction ->
                        counters.put(
                                reaction,
                                new LongAdder()
                        )
                );

        return counters;
    }
}
