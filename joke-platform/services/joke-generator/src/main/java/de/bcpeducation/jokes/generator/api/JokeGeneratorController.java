package de.bcpeducation.jokes.generator.api;

import de.bcpeducation.jokes.generator.domain.JokeSetup;
import de.bcpeducation.jokes.generator.service.JokeGeneratorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
public class JokeGeneratorController {

    private final JokeGeneratorService jokeGeneratorService;
    private final String instanceName;

    public JokeGeneratorController(
            JokeGeneratorService jokeGeneratorService,
            @Value("${joke-generator.instance-name:${HOSTNAME:local}}")
            String instanceName
    ) {
        this.jokeGeneratorService = jokeGeneratorService;
        this.instanceName = instanceName;
    }

    @GetMapping("/joke-setups/random")
    public JokeSetupResponse generateRandomSetup(
            @RequestParam(required = false)
            @Pattern(
                    regexp = "programming|kubernetes|dad|animal|science",
                    flags = Pattern.Flag.CASE_INSENSITIVE,
                    message = "category must be one of: programming, kubernetes, dad, animal, science"
            )
            String category,

            @RequestParam(required = false)
            @Min(
                    value = 0,
                    message = "seed must be greater than or equal to zero"
            )
            Long seed
    ) {
        JokeSetup jokeSetup = jokeGeneratorService.generate(category, seed);
        return JokeSetupResponse.from(jokeSetup, instanceName);
    }

    @PostMapping("/joke-setups/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public JokeSetupResponse generateSetup(
            @Valid @RequestBody GenerateJokeSetupRequest request
    ) {
        JokeSetup jokeSetup = jokeGeneratorService.generate(
                request.category(),
                request.seed()
        );

        return JokeSetupResponse.from(jokeSetup, instanceName);
    }

    @GetMapping("/joke-categories")
    public List<JokeCategoryResponse> getCategories() {
        return jokeGeneratorService.getCategories()
                .stream()
                .map(JokeCategoryResponse::from)
                .toList();
    }
}
