package de.bcpeducation.jokes.gateway.service;

import de.bcpeducation.jokes.gateway.client.AudienceReactionDto;
import de.bcpeducation.jokes.gateway.client.AudienceReactionRequestDto;
import de.bcpeducation.jokes.gateway.client.ChaosRequestDto;
import de.bcpeducation.jokes.gateway.client.ChaosResponseDto;
import de.bcpeducation.jokes.gateway.client.JokePlatformClient;
import de.bcpeducation.jokes.gateway.client.JokeSetupDto;
import de.bcpeducation.jokes.gateway.client.PunchlineDto;
import de.bcpeducation.jokes.gateway.domain.CompleteJoke;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JokeOrchestrationServiceTest {

    @Test
    void shouldAssembleCompleteJokeWithoutChaos() {
        TestJokePlatformClient client =
                new TestJokePlatformClient();

        JokeOrchestrationService service =
                new JokeOrchestrationService(
                        client,
                        new SimpleMeterRegistry()
                );

        CompleteJoke joke =
                service.createCompleteJoke(
                        "kubernetes",
                        "none",
                        42L,
                        "gateway-test-pod"
                );

        assertThat(joke.setupId())
                .isEqualTo("kubernetes-001");

        assertThat(joke.setup())
                .contains("Kubernetes pod");

        assertThat(joke.punchline())
                .contains("container issues");

        assertThat(joke.audience().reaction())
                .isEqualTo("laughter");

        assertThat(joke.chaos().invoked())
                .isFalse();

        assertThat(joke.trace().gateway())
                .isEqualTo("gateway-test-pod");

        assertThat(joke.trace().jokeGenerator())
                .isEqualTo("generator-test-pod");

        assertThat(joke.trace().punchlineService())
                .isEqualTo("punchline-test-pod");

        assertThat(joke.trace().audienceService())
                .isEqualTo("audience-test-pod");
    }

    @Test
    void shouldInvokeChaosWhenRequested() {
        TestJokePlatformClient client =
                new TestJokePlatformClient();

        JokeOrchestrationService service =
                new JokeOrchestrationService(
                        client,
                        new SimpleMeterRegistry()
                );

        CompleteJoke joke =
                service.createCompleteJoke(
                        "kubernetes",
                        "weird",
                        42L,
                        "gateway-test-pod"
                );

        assertThat(joke.chaos().invoked())
                .isTrue();

        assertThat(joke.chaos().requestedMode())
                .isEqualTo("weird");

        assertThat(joke.chaos().appliedMode())
                .isEqualTo("weird");

        assertThat(joke.chaos().oddity())
                .containsEntry(
                        "bananaCount",
                        42
                );

        assertThat(joke.trace().chaosComedian())
                .isEqualTo("chaos-test-pod");
    }

    private static final class TestJokePlatformClient
            extends JokePlatformClient {

        private TestJokePlatformClient() {
            super(
                    org.springframework.web.client.RestClient.create(),
                    org.springframework.web.client.RestClient.create(),
                    org.springframework.web.client.RestClient.create(),
                    org.springframework.web.client.RestClient.create()
            );
        }

        @Override
        public JokeSetupDto generateSetup(
                String category,
                Long seed
        ) {
            return new JokeSetupDto(
                    "kubernetes-001",
                    "kubernetes",
                    "Why did the Kubernetes pod visit a therapist?",
                    "generator-test-pod"
            );
        }

        @Override
        public PunchlineDto resolvePunchline(
                String setupId
        ) {
            return new PunchlineDto(
                    "punchline-kubernetes-001",
                    setupId,
                    "kubernetes",
                    "It had too many unresolved container issues.",
                    "punchline-test-pod"
            );
        }

        @Override
        public AudienceReactionDto createReaction(
                AudienceReactionRequestDto request
        ) {
            return new AudienceReactionDto(
                    "reaction-001",
                    request.setupId(),
                    request.category(),
                    "laughter",
                    "The audience laughs enthusiastically.",
                    7,
                    Instant.parse(
                            "2026-07-20T12:00:00Z"
                    ),
                    "audience-test-pod",
                    true,
                    false
            );
        }

        @Override
        public ChaosResponseDto invokeChaos(
                ChaosRequestDto request
        ) {
            return new ChaosResponseDto(
                    "chaos-request-001",
                    request.mode(),
                    "weird",
                    "Something odd happened",
                    0,
                    Map.of(
                            "bananaCount",
                            42
                    ),
                    "chaos-test-pod",
                    Instant.parse(
                            "2026-07-20T12:00:00Z"
                    )
            );
        }
    }
}
