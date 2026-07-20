package de.bcpeducation.jokes.punchline.api;

import de.bcpeducation.jokes.punchline.domain.Punchline;
import de.bcpeducation.jokes.punchline.service.PunchlineService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/punchlines")
@Validated
public class PunchlineController {

    private final PunchlineService punchlineService;
    private final String instanceName;

    public PunchlineController(
            PunchlineService punchlineService,
            @Value(
                    "${punchline-service.instance-name:${HOSTNAME:local}}"
            )
            String instanceName
    ) {
        this.punchlineService = punchlineService;
        this.instanceName = instanceName;
    }

    @GetMapping("/{setupId}")
    public PunchlineResponse getBySetupId(
            @PathVariable
            @Pattern(
                    regexp = "[a-zA-Z0-9-]+",
                    message = """
                            setupId may contain only letters, \
                            numbers and hyphens
                            """
            )
            String setupId
    ) {
        Punchline punchline =
                punchlineService.resolve(setupId);

        return PunchlineResponse.from(
                punchline,
                instanceName
        );
    }

    @PostMapping("/resolve")
    public PunchlineResponse resolve(
            @Valid
            @RequestBody
            ResolvePunchlineRequest request
    ) {
        Punchline punchline =
                punchlineService.resolve(request.setupId());

        return PunchlineResponse.from(
                punchline,
                instanceName
        );
    }

    @GetMapping("/random")
    public PunchlineResponse getRandom(
            @RequestParam(required = false)
            @Pattern(
                    regexp = "programming|kubernetes|dad|animal|science",
                    flags = Pattern.Flag.CASE_INSENSITIVE,
                    message = """
                            category must be one of: programming, \
                            kubernetes, dad, animal, science
                            """
            )
            String category,

            @RequestParam(required = false)
            @Min(
                    value = 0,
                    message = """
                            seed must be greater than or equal to zero
                            """
            )
            Long seed
    ) {
        Punchline punchline =
                punchlineService.selectRandom(category, seed);

        return PunchlineResponse.from(
                punchline,
                instanceName
        );
    }

    @PostMapping("/random")
    public PunchlineResponse createRandom(
            @Valid
            @RequestBody
            RandomPunchlineRequest request
    ) {
        Punchline punchline =
                punchlineService.selectRandom(
                        request.category(),
                        request.seed()
                );

        return PunchlineResponse.from(
                punchline,
                instanceName
        );
    }
}
