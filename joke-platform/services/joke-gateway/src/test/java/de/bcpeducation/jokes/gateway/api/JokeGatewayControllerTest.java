package de.bcpeducation.jokes.gateway.api;

import de.bcpeducation.jokes.gateway.JokeGatewayApplication;
import de.bcpeducation.jokes.gateway.client.AudienceReactionDto;
import de.bcpeducation.jokes.gateway.client.AudienceStatisticsDto;
import de.bcpeducation.jokes.gateway.client.ChaosResponseDto;
import de.bcpeducation.jokes.gateway.client.JokePlatformClient;
import de.bcpeducation.jokes.gateway.client.JokeSetupDto;
import de.bcpeducation.jokes.gateway.client.PunchlineDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(
        classes = JokeGatewayApplication.class
)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "joke-gateway.instance-name=test-gateway-pod",
        "joke-platform.http.connect-timeout-ms=100",
        "joke-platform.http.read-timeout-ms=100",
        "joke-platform.services.joke-generator.base-url=http://localhost:18081",
        "joke-platform.services.punchline-service.base-url=http://localhost:18082",
        "joke-platform.services.audience-service.base-url=http://localhost:18083",
        "joke-platform.services.chaos-comedian.base-url=http://localhost:18084"
})
class JokeGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JokePlatformClient client;

    @Test
    void shouldRenderDashboard()
            throws Exception {

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(
                        view().name("dashboard")
                )
                .andExpect(
                        content().string(
                                org.hamcrest.Matchers.containsString(
                                        "Kubernetes Joke Platform"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                org.hamcrest.Matchers.containsString(
                                        "test-gateway-pod"
                                )
                        )
                );
    }

    @Test
    void shouldGenerateCompleteJoke()
            throws Exception {

        configureBackendResponses();

        mockMvc.perform(
                        post("/api/v1/jokes")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "category": "kubernetes",
                                          "chaosMode": "weird",
                                          "seed": 42
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.setupId")
                                .value("kubernetes-001")
                )
                .andExpect(
                        jsonPath("$.category")
                                .value("kubernetes")
                )
                .andExpect(
                        jsonPath("$.audience.reaction")
                                .value("laughter")
                )
                .andExpect(
                        jsonPath("$.chaos.invoked")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.chaos.appliedMode")
                                .value("weird")
                )
                .andExpect(
                        jsonPath("$.trace.gateway")
                                .value("test-gateway-pod")
                )
                .andExpect(
                        jsonPath("$.trace.jokeGenerator")
                                .value("generator-test-pod")
                )
                .andExpect(
                        jsonPath("$.trace.chaosComedian")
                                .value("chaos-test-pod")
                );
    }

    @Test
    void shouldRejectInvalidChaosMode()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/jokes")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "category": "kubernetes",
                                          "chaosMode": "destroy-everything"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Request validation failed"
                                )
                );
    }

    @Test
    void shouldReturnStatistics()
            throws Exception {

        when(client.getStatistics())
                .thenReturn(
                        new AudienceStatisticsDto(
                                10,
                                42,
                                4.2,
                                Map.of(
                                        "laughter",
                                        4L,
                                        "groan",
                                        2L
                                ),
                                "redis"
                        )
                );

        mockMvc.perform(
                        get("/api/v1/statistics")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalReactions")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.averageScore")
                                .value(4.2)
                )
                .andExpect(
                        jsonPath("$.statisticsProvider")
                                .value("redis")
                );
    }

    @Test
    void shouldResetStatistics()
            throws Exception {

        mockMvc.perform(
                        delete("/api/v1/statistics")
                )
                .andExpect(status().isNoContent());
    }

    private void configureBackendResponses() {
        when(
                client.generateSetup(
                        eq("kubernetes"),
                        eq(42L)
                )
        )
                .thenReturn(
                        new JokeSetupDto(
                                "kubernetes-001",
                                "kubernetes",
                                "Why did the Kubernetes pod visit a therapist?",
                                "generator-test-pod"
                        )
                );

        when(
                client.resolvePunchline(
                        "kubernetes-001"
                )
        )
                .thenReturn(
                        new PunchlineDto(
                                "punchline-kubernetes-001",
                                "kubernetes-001",
                                "kubernetes",
                                "It had too many unresolved container issues.",
                                "punchline-test-pod"
                        )
                );

        when(client.invokeChaos(any()))
                .thenReturn(
                        new ChaosResponseDto(
                                "chaos-001",
                                "weird",
                                "weird",
                                "Something strange happened",
                                0,
                                Map.of(
                                        "bananaCount",
                                        42
                                ),
                                "chaos-test-pod",
                                Instant.parse(
                                        "2026-07-20T12:00:00Z"
                                )
                        )
                );

        when(client.createReaction(any()))
                .thenReturn(
                        new AudienceReactionDto(
                                "reaction-001",
                                "kubernetes-001",
                                "kubernetes",
                                "laughter",
                                "The audience laughs enthusiastically.",
                                7,
                                Instant.parse(
                                        "2026-07-20T12:00:00Z"
                                ),
                                "audience-test-pod",
                                true,
                                false
                        )
                );
    }
}
