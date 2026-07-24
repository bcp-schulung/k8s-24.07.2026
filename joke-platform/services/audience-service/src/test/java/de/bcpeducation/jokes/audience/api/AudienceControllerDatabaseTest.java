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
        "audience.statistics.provider=database",
        "audience.events.enabled=false",
        "management.health.redis.enabled=false",
        "management.health.rabbit.enabled=false"
})
class AudienceControllerDatabaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateAudienceReactionAndPersistToDatabase()
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
                        jsonPath("$.reactedBy")
                                .value("test-audience-pod")
                )
                .andExpect(
                        jsonPath("$.statisticsRecorded")
                                .value(true)
                );

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
                                .value("database")
                )
                .andExpect(
                        jsonPath("$.reactions").isMap()
                );
    }
}
