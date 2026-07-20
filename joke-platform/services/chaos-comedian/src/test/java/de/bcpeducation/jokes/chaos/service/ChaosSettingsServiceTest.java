package de.bcpeducation.jokes.chaos.service;

import de.bcpeducation.jokes.chaos.api.UpdateChaosSettingsRequest;
import de.bcpeducation.jokes.chaos.domain.ChaosSettings;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChaosSettingsServiceTest {

    private final ChaosSettingsService service =
            new ChaosSettingsService();

    @Test
    void shouldReturnDefaultSettings() {
        ChaosSettings settings =
                service.getSettings();

        assertThat(settings.failurePercentage())
                .isEqualTo(15);

        assertThat(settings.delayPercentage())
                .isEqualTo(30);

        assertThat(settings.weirdPercentage())
                .isEqualTo(15);

        assertThat(settings.normalPercentage())
                .isEqualTo(40);
    }

    @Test
    void shouldPartiallyUpdateSettings() {
        ChaosSettings updated =
                service.update(
                        new UpdateChaosSettingsRequest(
                                20,
                                null,
                                null,
                                100L,
                                500L
                        )
                );

        assertThat(updated.failurePercentage())
                .isEqualTo(20);

        assertThat(updated.delayPercentage())
                .isEqualTo(30);

        assertThat(updated.minimumDelayMs())
                .isEqualTo(100);

        assertThat(updated.maximumDelayMs())
                .isEqualTo(500);
    }

    @Test
    void shouldRejectPercentagesAboveOneHundredCombined() {
        assertThatThrownBy(() ->
                service.update(
                        new UpdateChaosSettingsRequest(
                                60,
                                30,
                                20,
                                null,
                                null
                        )
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "must not exceed 100"
                );
    }

    @Test
    void shouldResetSettings() {
        service.update(
                new UpdateChaosSettingsRequest(
                        50,
                        10,
                        10,
                        10L,
                        20L
                )
        );

        ChaosSettings reset =
                service.reset();

        assertThat(reset)
                .isEqualTo(
                        ChaosSettings.defaults()
                );
    }
}
