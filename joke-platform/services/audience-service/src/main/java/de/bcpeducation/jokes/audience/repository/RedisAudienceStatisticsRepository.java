package de.bcpeducation.jokes.audience.repository;

import de.bcpeducation.jokes.audience.domain.AudienceReaction;
import de.bcpeducation.jokes.audience.domain.AudienceStatistics;
import de.bcpeducation.jokes.audience.domain.ReactionResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Repository
@ConditionalOnProperty(
        name = "audience.statistics.provider",
        havingValue = "redis"
)
public class RedisAudienceStatisticsRepository
        implements AudienceStatisticsRepository {

    private static final String KEY_PREFIX =
            "joke-platform:audience";

    private static final String TOTAL_REACTIONS_KEY =
            KEY_PREFIX + ":total-reactions";

    private static final String TOTAL_SCORE_KEY =
            KEY_PREFIX + ":total-score";

    private static final String REACTION_KEY_PREFIX =
            KEY_PREFIX + ":reaction:";

    private final StringRedisTemplate redisTemplate;

    public RedisAudienceStatisticsRepository(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void record(ReactionResult reactionResult) {
        redisTemplate
                .opsForValue()
                .increment(TOTAL_REACTIONS_KEY);

        redisTemplate
                .opsForValue()
                .increment(
                        TOTAL_SCORE_KEY,
                        reactionResult.score()
                );

        redisTemplate
                .opsForValue()
                .increment(
                        reactionKey(
                                reactionResult.reaction()
                        )
                );
    }

    @Override
    public AudienceStatistics getStatistics() {
        long totalReactions =
                readLong(TOTAL_REACTIONS_KEY);

        long totalScore =
                readLong(TOTAL_SCORE_KEY);

        double averageScore = totalReactions == 0
                ? 0.0
                : (double) totalScore / totalReactions;

        Map<String, Long> reactions =
                new LinkedHashMap<>();

        Arrays.stream(AudienceReaction.values())
                .forEach(reaction ->
                        reactions.put(
                                reaction.value(),
                                readLong(reactionKey(reaction))
                        )
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
        Set<String> keys =
                redisTemplate.keys(KEY_PREFIX + ":*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public String providerName() {
        return "redis";
    }

    private long readLong(String key) {
        String value =
                redisTemplate
                        .opsForValue()
                        .get(key);

        if (value == null || value.isBlank()) {
            return 0;
        }

        return Long.parseLong(value);
    }

    private String reactionKey(
            AudienceReaction reaction
    ) {
        return REACTION_KEY_PREFIX + reaction.value();
    }
}
