package de.bcpeducation.jokes.generator.service;

import de.bcpeducation.jokes.generator.domain.JokeCategory;
import de.bcpeducation.jokes.generator.domain.JokeSetup;
import de.bcpeducation.jokes.generator.error.JokeCategoryNotFoundException;
import de.bcpeducation.jokes.generator.repository.JokeSetupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JokeGeneratorServiceTest {

    private JokeGeneratorService service;

    @BeforeEach
    void setUp() {
        JokeSetupRepository repository = new TestJokeSetupRepository();
        service = new JokeGeneratorService(repository);
    }

    @Test
    void shouldGenerateAJokeFromAnyCategory() {
        JokeSetup result = service.generate(null, 42L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotBlank();
        assertThat(result.text()).isNotBlank();
    }

    @Test
    void shouldGenerateAJokeFromRequestedCategory() {
        JokeSetup result = service.generate("kubernetes", 42L);

        assertThat(result.category()).isEqualTo(JokeCategory.KUBERNETES);
    }

    @Test
    void shouldReturnSameJokeForSameSeed() {
        JokeSetup first = service.generate("programming", 123L);
        JokeSetup second = service.generate("programming", 123L);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldRejectUnknownCategory() {
        assertThatThrownBy(() -> service.generate("politics", null))
                .isInstanceOf(JokeCategoryNotFoundException.class)
                .hasMessageContaining("politics");
    }

    @Test
    void shouldReturnAllCategories() {
        assertThat(service.getCategories())
                .containsExactly(JokeCategory.values());
    }

    private static final class TestJokeSetupRepository
            implements JokeSetupRepository {

        private final List<JokeSetup> jokes = List.of(
                new JokeSetup(
                        "programming-1",
                        JokeCategory.PROGRAMMING,
                        "Programming setup one"
                ),
                new JokeSetup(
                        "programming-2",
                        JokeCategory.PROGRAMMING,
                        "Programming setup two"
                ),
                new JokeSetup(
                        "kubernetes-1",
                        JokeCategory.KUBERNETES,
                        "Kubernetes setup one"
                )
        );

        @Override
        public List<JokeSetup> findAll() {
            return jokes;
        }

        @Override
        public List<JokeSetup> findByCategory(JokeCategory category) {
            return jokes.stream()
                    .filter(joke -> joke.category() == category)
                    .toList();
        }
    }
}
