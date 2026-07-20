package de.bcpeducation.jokes.audience.repository;

import de.bcpeducation.jokes.audience.domain.AudienceStatistics;
import de.bcpeducation.jokes.audience.domain.ReactionResult;

public interface AudienceStatisticsRepository {

    void record(ReactionResult reactionResult);

    AudienceStatistics getStatistics();

    void reset();

    String providerName();
}
