package de.bcpeducation.jokes.chaos.api;

import de.bcpeducation.jokes.chaos.ChaosComedianApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ChaosComedianApplication.class
)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "chaos-comedian.instance-name=test-chaos-pod",
        "chaos.termination.enabled=false"
})
class ChaosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetSettings() throws Exception {
        mockMvc.perform(
                        delete("/api/v1/chaos/settings")
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNormalResponse()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/chaos/perform")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "mode": "normal",
                                          "message": "Tell me something funny",
                                          "seed": 42
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.requestedMode")
                                .value("normal")
                )
                .andExpect(
                        jsonPath("$.appliedMode")
                                .value("normal")
                )
                .andExpect(
                        jsonPath("$.handledBy")
                                .value("test-chaos-pod")
                )
                .andExpect(
                        jsonPath("$.requestId")
                                .isNotEmpty()
                );
    }

    @Test
    void shouldReturnWeirdResponse()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/chaos/perform")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "mode": "weird",
                                          "seed": 42
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.appliedMode")
                                .value("weird")
                )
                .andExpect(
                        jsonPath("$.oddity.bananaCount")
                                .isNumber()
                )
                .andExpect(
                        jsonPath("$.oddity.unexpectedMessage")
                                .isNotEmpty()
                );
    }

    @Test
    void shouldReturnSimulatedServerError()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/chaos/perform")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "mode": "error",
                                          "seed": 42
                                        }
                                        """)
                )
                .andExpect(
                        status().isInternalServerError()
                )
                .andExpect(
                        jsonPath("$.status").value(500)
                )
                .andExpect(
                        jsonPath("$.details.simulated")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.requestId")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value("test-chaos-pod")
                );
    }

    @Test
    void shouldUpdateSettings()
            throws Exception {

        mockMvc.perform(
                        put("/api/v1/chaos/settings")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "failurePercentage": 20,
                                          "delayPercentage": 20,
                                          "weirdPercentage": 20,
                                          "minimumDelayMs": 50,
                                          "maximumDelayMs": 500
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.failurePercentage")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.delayPercentage")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.weirdPercentage")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.normalPercentage")
                                .value(40)
                );
    }

    @Test
    void shouldRejectInvalidCombinedPercentages()
            throws Exception {

        mockMvc.perform(
                        put("/api/v1/chaos/settings")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "failurePercentage": 50,
                                          "delayPercentage": 40,
                                          "weirdPercentage": 30
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "The combined chaos percentages must not exceed 100"
                                )
                );
    }

    @Test
    void shouldExposeSettings()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/chaos/settings")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.failurePercentage")
                                .value(15)
                )
                .andExpect(
                        jsonPath("$.normalPercentage")
                                .value(40)
                );
    }

    @Test
    void shouldRejectTerminationWhenDisabled()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/chaos/terminate")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "delayMs": 1000,
                                          "reason": "Integration test"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.details.environmentVariable")
                                .value(
                                        "CHAOS_TERMINATION_ENABLED"
                                )
                );
    }

    @Test
    void shouldRejectUnknownMode()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/chaos/perform")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "mode": "explode"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Request validation failed"
                                )
                );
    }
}
