package de.bcpeducation.jokes.audience.service;

import de.bcpeducation.jokes.audience.domain.AudienceStatistics;
import de.bcpeducation.jokes.audience.domain.CompletedJoke;
import de.bcpeducation.jokes.audience.domain.ReactionResult;
import de.bcpeducation.jokes.audience.event.AudienceReactionEvent;
import de.bcpeducation.jokes.audience.event.ReactionEventPublisher;
import de.bcpeducation.jokes.audience.repository.AudienceStatisticsRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceServiceTest {

    @Test
    void shouldRecordAndPublishReaction() {
        TestStatisticsRepository statisticsRepository =
                new TestStatisticsRepository();

        TestEventPublisher eventPublisher =
                new TestEventPublisher();

        AudienceService audienceService =
                new AudienceService(
                        new AudienceReactionService(),
                        statisticsRepository,
                        eventPublisher
                );

        CompletedJoke joke = new CompletedJoke(
                "programming-001",
                "programming",
                "Why did the developer go broke?",
                "Because they used up all their cache."
        );

        AudienceService.AudienceResult result =
                audienceService.react(
                        joke,
                        42L,
                        "test-pod"
                );

        assertThat(result.statisticsRecorded()).isTrue();
        assertThat(result.eventPublished()).isTrue();
        assertThat(statisticsRepository.results)
                .hasSize(1);
        assertThat(eventPublisher.events)
                .hasSize(1);
    }

    private static final class
            TestStatisticsRepository
            implements AudienceStatisticsRepository {

        private final List<ReactionResult> results =
                new ArrayList<>();

        @Override
        public void record(
                ReactionResult reactionResult
        ) {
            results.add(reactionResult);
        }

        @Override
        public AudienceStatistics getStatistics() {
            return AudienceStatistics.empty();
        }

        @Override
        public void reset() {
            results.clear();
        }

        @Override
        public String providerName() {
            return "test";
        }
    }

    private static final class TestEventPublisher
            implements ReactionEventPublisher {

        private final List<AudienceReactionEvent> events =
                new ArrayList<>();

        @Override
        public boolean publish(
                AudienceReactionEvent event
        ) {
            events.add(event);
            return true;
        }
    }
}
