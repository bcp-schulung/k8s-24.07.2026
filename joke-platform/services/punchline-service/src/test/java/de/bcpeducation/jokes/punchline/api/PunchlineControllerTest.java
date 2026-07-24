package de.bcpeducation.jokes.punchline.api;

import de.bcpeducation.jokes.punchline.PunchlineServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = PunchlineServiceApplication.class
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "punchline-service.instance-name=test-punchline-pod"
})
class PunchlineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldResolvePunchlineBySetupId()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/punchlines/kubernetes-001"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.setupId")
                                .value("kubernetes-001")
                )
                .andExpect(
                        jsonPath("$.category")
                                .value("kubernetes")
                )
                .andExpect(
                        jsonPath("$.text").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.resolvedBy")
                                .value("test-punchline-pod")
                );
    }

    @Test
    void shouldResolvePunchlineUsingPost()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/punchlines/resolve")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "setupId": "dad-002"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.setupId")
                                .value("dad-002")
                )
                .andExpect(
                        jsonPath("$.text")
                                .value("Nacho cheese.")
                );
    }

    @Test
    void shouldReturnNotFoundForUnknownSetup()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/punchlines/unknown-001"
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status").value(404)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "No punchline exists for setup ID: unknown-001"
                                )
                );
    }

    @Test
    void shouldReturnRandomCategoryPunchline()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/punchlines/random")
                                .queryParam(
                                        "category",
                                        "science"
                                )
                                .queryParam("seed", "42")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.category")
                                .value("science")
                )
                .andExpect(
                        jsonPath("$.resolvedBy")
                                .value("test-punchline-pod")
                );
    }

    @Test
    void shouldReturnSamePunchlineForSameSeed()
            throws Exception {

        String firstResponse =
                mockMvc.perform(
                                get(
                                        "/api/v1/punchlines/random"
                                )
                                        .queryParam(
                                                "category",
                                                "programming"
                                        )
                                        .queryParam(
                                                "seed",
                                                "123"
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String secondResponse =
                mockMvc.perform(
                                get(
                                        "/api/v1/punchlines/random"
                                )
                                        .queryParam(
                                                "category",
                                                "programming"
                                        )
                                        .queryParam(
                                                "seed",
                                                "123"
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        org.assertj.core.api.Assertions
                .assertThat(secondResponse)
                .isEqualTo(firstResponse);
    }

    @Test
    void shouldRejectInvalidCategory()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/punchlines/random")
                                .queryParam(
                                        "category",
                                        "unknown"
                                )
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
                );
    }

    @Test
    void shouldRejectBlankPostSetupId()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/punchlines/resolve")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "setupId": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.violations").isArray()
                );
    }
}
