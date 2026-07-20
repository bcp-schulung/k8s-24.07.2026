package de.bcpeducation.jokes.chaos.service;

import de.bcpeducation.jokes.chaos.api.UpdateChaosSettingsRequest;
import de.bcpeducation.jokes.chaos.domain.ChaosSettings;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChaosSettingsService {

    private final AtomicReference<ChaosSettings> settings =
            new AtomicReference<>(
                    ChaosSettings.defaults()
            );

    public ChaosSettings getSettings() {
        return settings.get();
    }

    public ChaosSettings update(
            UpdateChaosSettingsRequest request
    ) {
        ChaosSettings current = settings.get();

        ChaosSettings updated = new ChaosSettings(
                valueOrCurrent(
                        request.failurePercentage(),
                        current.failurePercentage()
                ),
                valueOrCurrent(
                        request.delayPercentage(),
                        current.delayPercentage()
                ),
                valueOrCurrent(
                        request.weirdPercentage(),
                        current.weirdPercentage()
                ),
                valueOrCurrent(
                        request.minimumDelayMs(),
                        current.minimumDelayMs()
                ),
                valueOrCurrent(
                        request.maximumDelayMs(),
                        current.maximumDelayMs()
                )
        );

        settings.set(updated);
        return updated;
    }

    public ChaosSettings reset() {
        ChaosSettings defaults =
                ChaosSettings.defaults();

        settings.set(defaults);
        return defaults;
    }

    private int valueOrCurrent(
            Integer requested,
            int current
    ) {
        return requested == null
                ? current
                : requested;
    }

    private long valueOrCurrent(
            Long requested,
            long current
    ) {
        return requested == null
                ? current
                : requested;
    }
}
