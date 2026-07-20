package de.bcpeducation.jokes.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joke-platform.services")
public record BackendServiceProperties(
        ServiceEndpoint jokeGenerator,
        ServiceEndpoint punchlineService,
        ServiceEndpoint audienceService,
        ServiceEndpoint chaosComedian
) {

    public BackendServiceProperties {
        requireEndpoint(jokeGenerator, "jokeGenerator");
        requireEndpoint(punchlineService, "punchlineService");
        requireEndpoint(audienceService, "audienceService");
        requireEndpoint(chaosComedian, "chaosComedian");
    }

    private static void requireEndpoint(
            ServiceEndpoint endpoint,
            String name
    ) {
        if (endpoint == null) {
            throw new IllegalArgumentException(
                    name + " configuration is required"
            );
        }
    }

    public record ServiceEndpoint(
            String baseUrl
    ) {

        public ServiceEndpoint {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalArgumentException(
                        "Service base URL must not be blank"
                );
            }
        }
    }
}
