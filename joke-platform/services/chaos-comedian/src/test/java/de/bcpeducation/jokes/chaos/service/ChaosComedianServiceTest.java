package de.bcpeducation.jokes.chaos.service;

import de.bcpeducation.jokes.chaos.api.ChaosResponse;
import de.bcpeducation.jokes.chaos.error.SimulatedChaosException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChaosComedianServiceTest {

    private final ChaosSettingsService settingsService =
            new ChaosSettingsService();

    private final ChaosComedianService service =
            new ChaosComedianService(
                    settingsService,
                    new SimpleMeterRegistry()
            );

    @Test
    void shouldReturnNormalResponse() {
        ChaosResponse response =
                service.perform(
                        "normal",
                        "Tell the joke",
                        42L,
                        "test-pod"
                );

        assertThat(response.requestedMode())
                .isEqualTo("normal");

        assertThat(response.appliedMode())
                .isEqualTo("normal");

        assertThat(response.handledBy())
                .isEqualTo("test-pod");

        assertThat(response.delayMs())
                .isZero();
    }

    @Test
    void shouldReturnDelayedResponse() {
        settingsService.update(
                new de.bcpeducation.jokes.chaos.api.UpdateChaosSettingsRequest(
                        0,
                        100,
                        0,
                        1L,
                        1L
                )
        );

        ChaosResponse response =
                service.perform(
                        "delay",
                        "Wait for it",
                        42L,
                        "test-pod"
                );

        assertThat(response.appliedMode())
                .isEqualTo("delay");

        assertThat(response.delayMs())
                .isEqualTo(1);
    }

    @Test
    void shouldReturnWeirdResponse() {
        ChaosResponse response =
                service.perform(
                        "weird",
                        "Something odd",
                        42L,
                        "test-pod"
                );

        assertThat(response.appliedMode())
                .isEqualTo("weird");

        assertThat(response.oddity())
                .isNotEmpty();

        assertThat(response.oddity())
                .containsKey("bananaCount");
    }

    @Test
    void shouldThrowSimulatedFailure() {
        assertThatThrownBy(() ->
                service.perform(
                        "error",
                        "Break now",
                        42L,
                        "test-pod"
                )
        )
                .isInstanceOf(
                        SimulatedChaosException.class
                );
    }

    @Test
    void shouldApplyConfiguredRandomDistribution() {
        settingsService.update(
                new de.bcpeducation.jokes.chaos.api.UpdateChaosSettingsRequest(
                        0,
                        0,
                        0,
                        1L,
                        1L
                )
        );

        ChaosResponse response =
                service.perform(
                        "random",
                        "Random request",
                        999L,
                        "test-pod"
                );

        assertThat(response.requestedMode())
                .isEqualTo("random");

        assertThat(response.appliedMode())
                .isEqualTo("normal");
    }
}
