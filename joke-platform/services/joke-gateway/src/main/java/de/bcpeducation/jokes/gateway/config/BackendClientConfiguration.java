package de.bcpeducation.jokes.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        BackendServiceProperties.class,
        GatewayHttpProperties.class
})
public class BackendClientConfiguration {

    @Bean
    @Qualifier("jokeGeneratorRestClient")
    public RestClient jokeGeneratorRestClient(
            BackendServiceProperties serviceProperties,
            GatewayHttpProperties httpProperties
    ) {
        return createRestClient(
                serviceProperties
                        .jokeGenerator()
                        .baseUrl(),
                httpProperties
        );
    }

    @Bean
    @Qualifier("punchlineRestClient")
    public RestClient punchlineRestClient(
            BackendServiceProperties serviceProperties,
            GatewayHttpProperties httpProperties
    ) {
        return createRestClient(
                serviceProperties
                        .punchlineService()
                        .baseUrl(),
                httpProperties
        );
    }

    @Bean
    @Qualifier("audienceRestClient")
    public RestClient audienceRestClient(
            BackendServiceProperties serviceProperties,
            GatewayHttpProperties httpProperties
    ) {
        return createRestClient(
                serviceProperties
                        .audienceService()
                        .baseUrl(),
                httpProperties
        );
    }

    @Bean
    @Qualifier("chaosRestClient")
    public RestClient chaosRestClient(
            BackendServiceProperties serviceProperties,
            GatewayHttpProperties httpProperties
    ) {
        return createRestClient(
                serviceProperties
                        .chaosComedian()
                        .baseUrl(),
                httpProperties
        );
    }

    private RestClient createRestClient(
            String baseUrl,
            GatewayHttpProperties properties
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
                properties.connectTimeoutMs()
        );

        requestFactory.setReadTimeout(
                properties.readTimeoutMs()
        );

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(
                        "X-Joke-Platform-Client",
                        "joke-gateway"
                )
                .build();
    }
}
