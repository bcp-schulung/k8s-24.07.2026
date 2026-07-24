package de.bcpeducation.jokes.audience.repository;

import de.bcpeducation.jokes.audience.domain.AudienceReaction;
import de.bcpeducation.jokes.audience.domain.AudienceStatistics;
import de.bcpeducation.jokes.audience.domain.ReactionResult;
import de.bcpeducation.jokes.audience.entity.ReactionEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Repository
@ConditionalOnProperty(
        name = "audience.statistics.provider",
        havingValue = "database"
)
public class DatabaseAudienceStatisticsRepository
        implements AudienceStatisticsRepository {

    private final ReactionJpaRepository reactionJpaRepository;

    public DatabaseAudienceStatisticsRepository(
            ReactionJpaRepository reactionJpaRepository
    ) {
        this.reactionJpaRepository = reactionJpaRepository;
    }

    @Override
    public void record(ReactionResult reactionResult) {
        ReactionEntity entity = new ReactionEntity();
        entity.setReactionId(UUID.fromString(reactionResult.reactionId()));
        entity.setSetupId(reactionResult.setupId());
        entity.setCategory(reactionResult.category());
        entity.setReaction(reactionResult.reaction().value());
        entity.setScore(reactionResult.score());
        entity.setReactedAt(reactionResult.reactedAt());

        reactionJpaRepository.save(entity);
    }

    @Override
    public AudienceStatistics getStatistics() {
        long totalReactions = reactionJpaRepository.count();
        long totalScore = reactionJpaRepository.sumScore();

        double averageScore = totalReactions == 0
                ? 0.0
                : (double) totalScore / totalReactions;

        Map<String, Long> reactions = new LinkedHashMap<>();
        reactionJpaRepository
                .countByReaction()
                .forEach(count ->
                        reactions.put(count.reaction(), count.count())
                );

        Arrays.stream(AudienceReaction.values())
                .map(AudienceReaction::value)
                .forEach(value ->
                        reactions.putIfAbsent(value, 0L)
                );

        return new AudienceStatistics(
                totalReactions,
                totalScore,
                averageScore,
                reactions
        );
    }

    @Override
    public void reset() {
        reactionJpaRepository.deleteAll();
    }

    @Override
    public String providerName() {
        return "database";
    }
}
