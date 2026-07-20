package de.bcpeducation.jokes.gateway.api;

import de.bcpeducation.jokes.gateway.client.AudienceStatisticsDto;
import de.bcpeducation.jokes.gateway.client.JokeCategoryDto;
import de.bcpeducation.jokes.gateway.client.JokePlatformClient;
import de.bcpeducation.jokes.gateway.domain.CompleteJoke;
import de.bcpeducation.jokes.gateway.service.JokeOrchestrationService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
public class JokeApiController {

    private final JokeOrchestrationService orchestrationService;
    private final JokePlatformClient client;
    private final String instanceName;

    public JokeApiController(
            JokeOrchestrationService orchestrationService,
            JokePlatformClient client,
            @Value(
                    "${joke-gateway.instance-name:${HOSTNAME:local}}"
            )
            String instanceName
    ) {
        this.orchestrationService = orchestrationService;
        this.client = client;
        this.instanceName = instanceName;
    }

    @PostMapping("/jokes")
    @ResponseStatus(HttpStatus.CREATED)
    public CompleteJokeResponse generateJoke(
            @Valid
            @RequestBody
            GenerateCompleteJokeRequest request
    ) {
        CompleteJoke joke =
                orchestrationService.createCompleteJoke(
                        request.category(),
                        request.chaosMode(),
                        request.seed(),
                        instanceName
                );

        return CompleteJokeResponse.from(joke);
    }

    @GetMapping("/categories")
    public List<JokeCategoryDto> getCategories() {
        return client.getCategories();
    }

    @GetMapping("/statistics")
    public AudienceStatisticsDto getStatistics() {
        return client.getStatistics();
    }

    @DeleteMapping("/statistics")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetStatistics() {
        client.resetStatistics();
    }
}
