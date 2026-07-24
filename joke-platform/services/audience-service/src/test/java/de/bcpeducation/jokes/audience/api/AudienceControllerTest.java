package de.bcpeducation.jokes.audience.api;

import de.bcpeducation.jokes.audience.AudienceServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AudienceServiceApplication.class
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "audience-service.instance-name=test-audience-pod",
        "audience.statistics.provider=memory",
        "audience.events.enabled=false",
        "management.health.redis.enabled=false",
        "management.health.rabbit.enabled=false"
})
class AudienceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateAudienceReaction()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/audience/reactions")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "setupId": "kubernetes-001",
                                          "category": "kubernetes",
                                          "setup": "Why did the Kubernetes pod visit a therapist?",
                                          "punchline": "It had too many unresolved container issues.",
                                          "seed": 42
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.reactionId").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.setupId")
                                .value("kubernetes-001")
                )
                .andExpect(
                        jsonPath("$.category")
                                .value("kubernetes")
                )
                .andExpect(
                        jsonPath("$.reaction").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.score").isNumber()
                )
                .andExpect(
                        jsonPath("$.reactedBy")
                                .value("test-audience-pod")
                )
                .andExpect(
                        jsonPath("$.statisticsRecorded")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.eventPublished")
                                .value(false)
                );
    }

    @Test
    void shouldRejectBlankPunchline()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/audience/reactions")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "setupId": "dad-001",
                                          "category": "dad",
                                          "setup": "Why could the bicycle not stand?",
                                          "punchline": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Request validation failed"
                                )
                )
                .andExpect(
                        jsonPath("$.violations").isArray()
                );
    }

    @Test
    void shouldReturnStatistics()
            throws Exception {

        createReaction();

        mockMvc.perform(
                        get("/api/v1/audience/statistics")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalReactions")
                                .value(
                                        greaterThanOrEqualTo(1)
                                )
                )
                .andExpect(
                        jsonPath("$.statisticsProvider")
                                .value("memory")
                )
                .andExpect(
                        jsonPath("$.reactions").isMap()
                );
    }

    @Test
    void shouldResetStatistics()
            throws Exception {

        createReaction();

        mockMvc.perform(
                        delete(
                                "/api/v1/audience/statistics"
                        )
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/audience/statistics")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalReactions")
                                .value(0)
                );
    }

    private void createReaction() throws Exception {
        mockMvc.perform(
                        post("/api/v1/audience/reactions")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "setupId": "science-001",
                                          "category": "science",
                                          "setup": "Why can you never trust an atom?",
                                          "punchline": "Because they make up everything.",
                                          "seed": 123
                                        }
                                        """)
                )
                .andExpect(status().isCreated());
    }
}
