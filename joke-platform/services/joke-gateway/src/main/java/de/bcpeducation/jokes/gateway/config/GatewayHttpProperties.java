package de.bcpeducation.jokes.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joke-platform.http")
public record GatewayHttpProperties(
        int connectTimeoutMs,
        int readTimeoutMs
) {

    public GatewayHttpProperties {
        if (connectTimeoutMs < 1) {
            throw new IllegalArgumentException(
                    "connectTimeoutMs must be positive"
            );
        }

        if (readTimeoutMs < 1) {
            throw new IllegalArgumentException(
                    "readTimeoutMs must be positive"
            );
        }
    }
}
