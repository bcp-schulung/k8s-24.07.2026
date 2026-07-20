package de.bcpeducation.jokes.audience.api;

import de.bcpeducation.jokes.audience.domain.AudienceStatistics;
import de.bcpeducation.jokes.audience.domain.CompletedJoke;
import de.bcpeducation.jokes.audience.service.AudienceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audience")
@Validated
public class AudienceController {

    private final AudienceService audienceService;
    private final String instanceName;

    public AudienceController(
            AudienceService audienceService,
            @Value(
                    "${audience-service.instance-name:${HOSTNAME:local}}"
            )
            String instanceName
    ) {
        this.audienceService = audienceService;
        this.instanceName = instanceName;
    }

    @PostMapping("/reactions")
    @ResponseStatus(HttpStatus.CREATED)
    public ReactionResponse createReaction(
            @Valid
            @RequestBody
            CreateReactionRequest request
    ) {
        CompletedJoke joke = new CompletedJoke(
                request.setupId(),
                request.category(),
                request.setup(),
                request.punchline()
        );

        AudienceService.AudienceResult result =
                audienceService.react(
                        joke,
                        request.seed(),
                        instanceName
                );

        return ReactionResponse.from(
                result.reaction(),
                instanceName,
                result.statisticsRecorded(),
                result.eventPublished()
        );
    }

    @GetMapping("/statistics")
    public AudienceStatisticsResponse getStatistics() {
        AudienceStatistics statistics =
                audienceService.getStatistics();

        return AudienceStatisticsResponse.from(
                statistics,
                audienceService.statisticsProviderName()
        );
    }

    @DeleteMapping("/statistics")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetStatistics() {
        audienceService.resetStatistics();
    }
}
