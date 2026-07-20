package de.bcpeducation.jokes.chaos.api;

import de.bcpeducation.jokes.chaos.domain.ChaosSettings;
import de.bcpeducation.jokes.chaos.error.InvalidChaosSettingsException;
import de.bcpeducation.jokes.chaos.service.ApplicationTerminationService;
import de.bcpeducation.jokes.chaos.service.ChaosComedianService;
import de.bcpeducation.jokes.chaos.service.ChaosSettingsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/chaos")
@Validated
public class ChaosController {

    private final ChaosComedianService chaosComedianService;
    private final ChaosSettingsService settingsService;
    private final ApplicationTerminationService terminationService;
    private final String instanceName;

    public ChaosController(
            ChaosComedianService chaosComedianService,
            ChaosSettingsService settingsService,
            ApplicationTerminationService terminationService,
            @Value(
                    "${chaos-comedian.instance-name:${HOSTNAME:local}}"
            )
            String instanceName
    ) {
        this.chaosComedianService = chaosComedianService;
        this.settingsService = settingsService;
        this.terminationService = terminationService;
        this.instanceName = instanceName;
    }

    @PostMapping("/perform")
    public ChaosResponse perform(
            @Valid
            @RequestBody
            ChaosRequest request
    ) {
        return chaosComedianService.perform(
                request.mode(),
                request.message(),
                request.seed(),
                instanceName
        );
    }

    @GetMapping("/settings")
    public ChaosSettingsResponse getSettings() {
        return ChaosSettingsResponse.from(
                settingsService.getSettings()
        );
    }

    @PutMapping("/settings")
    public ChaosSettingsResponse updateSettings(
            @Valid
            @RequestBody
            UpdateChaosSettingsRequest request
    ) {
        try {
            ChaosSettings updated =
                    settingsService.update(request);

            return ChaosSettingsResponse.from(updated);
        } catch (IllegalArgumentException exception) {
            throw new InvalidChaosSettingsException(
                    exception.getMessage()
            );
        }
    }

    @DeleteMapping("/settings")
    public ChaosSettingsResponse resetSettings() {
        return ChaosSettingsResponse.from(
                settingsService.reset()
        );
    }

    @GetMapping("/termination")
    public TerminationStatusResponse getTerminationStatus() {
        return new TerminationStatusResponse(
                terminationService.isTerminationEnabled(),
                instanceName
        );
    }

    @PostMapping("/terminate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TerminationResponse terminate(
            @Valid
            @RequestBody
            TerminationRequest request
    ) {
        long delayMs =
                request.delayMs() == null
                        ? 1_000
                        : request.delayMs();

        String reason =
                request.reason() == null
                        || request.reason().isBlank()
                        ? "Kubernetes self-healing demonstration"
                        : request.reason().trim();

        terminationService.scheduleTermination(
                delayMs,
                reason
        );

        return new TerminationResponse(
                "Termination scheduled",
                instanceName,
                delayMs,
                reason,
                Instant.now()
        );
    }

    public record TerminationStatusResponse(
            boolean enabled,
            String instance
    ) {
    }
}
