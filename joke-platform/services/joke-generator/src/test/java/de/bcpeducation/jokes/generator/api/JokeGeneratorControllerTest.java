package de.bcpeducation.jokes.generator.api;

import de.bcpeducation.jokes.generator.JokeGeneratorApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JokeGeneratorApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "joke-generator.instance-name=test-instance"
})
class JokeGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateRandomJokeSetup() throws Exception {
        mockMvc.perform(
                        get("/api/v1/joke-setups/random")
                                .queryParam("category", "kubernetes")
                                .queryParam("seed", "42")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.category").value("kubernetes"))
                .andExpect(jsonPath("$.text").isNotEmpty())
                .andExpect(jsonPath("$.generatedBy")
                        .value("test-instance"));
    }

    @Test
    void shouldGenerateJokeSetupUsingPost() throws Exception {
        mockMvc.perform(
                        post("/api/v1/joke-setups/generate")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "category": "programming",
                                          "seed": 123
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("programming"))
                .andExpect(jsonPath("$.generatedBy")
                        .value("test-instance"));
    }

    @Test
    void shouldListCategories() throws Exception {
        mockMvc.perform(get("/api/v1/joke-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].value").value("programming"))
                .andExpect(jsonPath("$[1].value").value("kubernetes"));
    }

    @Test
    void shouldRejectInvalidCategory() throws Exception {
        mockMvc.perform(
                        get("/api/v1/joke-setups/random")
                                .queryParam("category", "unknown")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    void shouldRejectNegativeSeed() throws Exception {
        mockMvc.perform(
                        get("/api/v1/joke-setups/random")
                                .queryParam("seed", "-1")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
