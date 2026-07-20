package de.bcpeducation.jokes.gateway.client;

import de.bcpeducation.jokes.gateway.error.BackendServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class JokePlatformClient {

    private final RestClient jokeGeneratorClient;
    private final RestClient punchlineClient;
    private final RestClient audienceClient;
    private final RestClient chaosClient;

    public JokePlatformClient(
            @Qualifier("jokeGeneratorRestClient")
            RestClient jokeGeneratorClient,

            @Qualifier("punchlineRestClient")
            RestClient punchlineClient,

            @Qualifier("audienceRestClient")
            RestClient audienceClient,

            @Qualifier("chaosRestClient")
            RestClient chaosClient
    ) {
        this.jokeGeneratorClient = jokeGeneratorClient;
        this.punchlineClient = punchlineClient;
        this.audienceClient = audienceClient;
        this.chaosClient = chaosClient;
    }

    public JokeSetupDto generateSetup(
            String category,
            Long seed
    ) {
        try {
            JokeSetupDto response =
                    jokeGeneratorClient.get()
                            .uri(uriBuilder -> {
                                var builder = uriBuilder
                                        .path(
                                                "/api/v1/joke-setups/random"
                                        );

                                if (category != null
                                        && !category.isBlank()) {
                                    builder.queryParam(
                                            "category",
                                            category
                                    );
                                }

                                if (seed != null) {
                                    builder.queryParam(
                                            "seed",
                                            seed
                                    );
                                }

                                return builder.build();
                            })
                            .retrieve()
                            .body(JokeSetupDto.class);

            return requireResponse(
                    response,
                    "joke-generator"
            );
        } catch (Exception exception) {
            throw translate(
                    "joke-generator",
                    exception
            );
        }
    }

    public List<JokeCategoryDto> getCategories() {
        try {
            List<JokeCategoryDto> response =
                    jokeGeneratorClient.get()
                            .uri("/api/v1/joke-categories")
                            .retrieve()
                            .body(
                                    new ParameterizedTypeReference<>() {
                                    }
                            );

            return response == null
                    ? List.of()
                    : List.copyOf(response);
        } catch (Exception exception) {
            throw translate(
                    "joke-generator",
                    exception
            );
        }
    }

    public PunchlineDto resolvePunchline(
            String setupId
    ) {
        try {
            PunchlineDto response =
                    punchlineClient.get()
                            .uri(
                                    "/api/v1/punchlines/{setupId}",
                                    setupId
                            )
                            .retrieve()
                            .body(PunchlineDto.class);

            return requireResponse(
                    response,
                    "punchline-service"
            );
        } catch (Exception exception) {
            throw translate(
                    "punchline-service",
                    exception
            );
        }
    }

    public AudienceReactionDto createReaction(
            AudienceReactionRequestDto request
    ) {
        try {
            AudienceReactionDto response =
                    audienceClient.post()
                            .uri(
                                    "/api/v1/audience/reactions"
                            )
                            .body(request)
                            .retrieve()
                            .body(AudienceReactionDto.class);

            return requireResponse(
                    response,
                    "audience-service"
            );
        } catch (Exception exception) {
            throw translate(
                    "audience-service",
                    exception
            );
        }
    }

    public AudienceStatisticsDto getStatistics() {
        try {
            AudienceStatisticsDto response =
                    audienceClient.get()
                            .uri(
                                    "/api/v1/audience/statistics"
                            )
                            .retrieve()
                            .body(
                                    AudienceStatisticsDto.class
                            );

            return requireResponse(
                    response,
                    "audience-service"
            );
        } catch (Exception exception) {
            throw translate(
                    "audience-service",
                    exception
            );
        }
    }

    public void resetStatistics() {
        try {
            audienceClient.delete()
                    .uri(
                            "/api/v1/audience/statistics"
                    )
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw translate(
                    "audience-service",
                    exception
            );
        }
    }

    public ChaosResponseDto invokeChaos(
            ChaosRequestDto request
    ) {
        try {
            ChaosResponseDto response =
                    chaosClient.post()
                            .uri("/api/v1/chaos/perform")
                            .body(request)
                            .retrieve()
                            .body(ChaosResponseDto.class);

            return requireResponse(
                    response,
                    "chaos-comedian"
            );
        } catch (Exception exception) {
            throw translate(
                    "chaos-comedian",
                    exception
            );
        }
    }

    private <T> T requireResponse(
            T response,
            String serviceName
    ) {
        if (response == null) {
            throw new BackendServiceException(
                    serviceName,
                    serviceName
                            + " returned an empty response",
                    null
            );
        }

        return response;
    }

    private BackendServiceException translate(
            String serviceName,
            Exception exception
    ) {
        if (exception
                instanceof BackendServiceException backendException) {
            return backendException;
        }

        if (exception
                instanceof RestClientResponseException responseException) {
            HttpStatusCode statusCode =
                    responseException.getStatusCode();

            return new BackendServiceException(
                    serviceName,
                    serviceName
                            + " returned HTTP "
                            + statusCode.value(),
                    statusCode.value(),
                    responseException
            );
        }

        if (exception
                instanceof ResourceAccessException) {
            return new BackendServiceException(
                    serviceName,
                    "Could not reach "
                            + serviceName
                            + " before the configured timeout",
                    exception
            );
        }

        return new BackendServiceException(
                serviceName,
                "Unexpected communication failure with "
                        + serviceName,
                exception
        );
    }
}
