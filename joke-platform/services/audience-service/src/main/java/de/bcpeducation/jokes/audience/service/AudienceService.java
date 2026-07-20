package de.bcpeducation.jokes.audience.service;

import de.bcpeducation.jokes.audience.domain.AudienceStatistics;
import de.bcpeducation.jokes.audience.domain.CompletedJoke;
import de.bcpeducation.jokes.audience.domain.ReactionResult;
import de.bcpeducation.jokes.audience.event.AudienceReactionEvent;
import de.bcpeducation.jokes.audience.event.ReactionEventPublisher;
import de.bcpeducation.jokes.audience.repository.AudienceStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class AudienceService {

    private static final Logger log =
            LoggerFactory.getLogger(AudienceService.class);

    private final AudienceReactionService reactionService;
    private final AudienceStatisticsRepository statisticsRepository;
    private final ReactionEventPublisher eventPublisher;

    public AudienceService(
            AudienceReactionService reactionService,
            AudienceStatisticsRepository statisticsRepository,
            ReactionEventPublisher eventPublisher
    ) {
        this.reactionService = reactionService;
        this.statisticsRepository = statisticsRepository;
        this.eventPublisher = eventPublisher;
    }

    public AudienceResult react(
            CompletedJoke joke,
            Long seed,
            String instanceName
    ) {
        ReactionResult reactionResult =
                reactionService.react(joke, seed);

        boolean statisticsRecorded =
                recordStatistics(reactionResult);

        boolean eventPublished =
                eventPublisher.publish(
                        AudienceReactionEvent.from(
                                reactionResult,
                                instanceName
                        )
                );

        return new AudienceResult(
                reactionResult,
                statisticsRecorded,
                eventPublished
        );
    }

    public AudienceStatistics getStatistics() {
        return statisticsRepository.getStatistics();
    }

    public void resetStatistics() {
        statisticsRepository.reset();
    }

    public String statisticsProviderName() {
        return statisticsRepository.providerName();
    }

    private boolean recordStatistics(
            ReactionResult reactionResult
    ) {
        try {
            statisticsRepository.record(reactionResult);
            return true;
        } catch (DataAccessException exception) {
            log.warn(
                    "Could not record reaction {} in {}",
                    reactionResult.reactionId(),
                    statisticsRepository.providerName(),
                    exception
            );

            return false;
        }
    }

    public record AudienceResult(
            ReactionResult reaction,
            boolean statisticsRecorded,
            boolean eventPublished
    ) {
    }
}
