package de.bcpeducation.jokes.audience.service;

import de.bcpeducation.jokes.audience.domain.CompletedJoke;
import de.bcpeducation.jokes.audience.domain.ReactionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceReactionServiceTest {

    private final AudienceReactionService service =
            new AudienceReactionService();

    private final CompletedJoke joke =
            new CompletedJoke(
                    "kubernetes-001",
                    "kubernetes",
                    "Why did the Kubernetes pod visit a therapist?",
                    "It had too many unresolved container issues."
            );

    @Test
    void shouldGenerateAReaction() {
        ReactionResult result =
                service.react(joke, 42L);

        assertThat(result.reactionId()).isNotBlank();
        assertThat(result.setupId())
                .isEqualTo("kubernetes-001");
        assertThat(result.category())
                .isEqualTo("kubernetes");
        assertThat(result.reaction()).isNotNull();
        assertThat(result.score())
                .isBetween(-5, 10);
        assertThat(result.reactedAt()).isNotNull();
    }

    @Test
    void shouldGenerateSameOutcomeForSameSeed() {
        ReactionResult first =
                service.react(joke, 123L);

        ReactionResult second =
                service.react(joke, 123L);

        assertThat(second.reaction())
                .isEqualTo(first.reaction());

        assertThat(second.score())
                .isEqualTo(first.score());
    }
}
