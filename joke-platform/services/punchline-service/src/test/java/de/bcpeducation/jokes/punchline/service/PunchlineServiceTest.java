package de.bcpeducation.jokes.punchline.service;

import de.bcpeducation.jokes.punchline.domain.JokeCategory;
import de.bcpeducation.jokes.punchline.domain.Punchline;
import de.bcpeducation.jokes.punchline.error.PunchlineNotFoundException;
import de.bcpeducation.jokes.punchline.repository.PunchlineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PunchlineServiceTest {

    private PunchlineService service;

    @BeforeEach
    void setUp() {
        service = new PunchlineService(
                new TestPunchlineRepository()
        );
    }

    @Test
    void shouldResolvePunchlineBySetupId() {
        Punchline result =
                service.resolve("kubernetes-001");

        assertThat(result.setupId())
                .isEqualTo("kubernetes-001");

        assertThat(result.category())
                .isEqualTo(JokeCategory.KUBERNETES);
    }

    @Test
    void shouldResolveSetupIdIgnoringCase() {
        Punchline result =
                service.resolve("KUBERNETES-001");

        assertThat(result.setupId())
                .isEqualTo("kubernetes-001");
    }

    @Test
    void shouldRejectUnknownSetupId() {
        assertThatThrownBy(() ->
                service.resolve("unknown-001")
        )
                .isInstanceOf(
                        PunchlineNotFoundException.class
                )
                .hasMessageContaining("unknown-001");
    }

    @Test
    void shouldSelectPunchlineFromRequestedCategory() {
        Punchline result =
                service.selectRandom(
                        "programming",
                        42L
                );

        assertThat(result.category())
                .isEqualTo(JokeCategory.PROGRAMMING);
    }

    @Test
    void shouldReturnSamePunchlineForSameSeed() {
        Punchline first =
                service.selectRandom(
                        "programming",
                        123L
                );

        Punchline second =
                service.selectRandom(
                        "programming",
                        123L
                );

        assertThat(first).isEqualTo(second);
    }

    private static final class TestPunchlineRepository
            implements PunchlineRepository {

        private final List<Punchline> punchlines =
                List.of(
                        new Punchline(
                                "punchline-programming-001",
                                "programming-001",
                                JokeCategory.PROGRAMMING,
                                "First programming punchline"
                        ),
                        new Punchline(
                                "punchline-programming-002",
                                "programming-002",
                                JokeCategory.PROGRAMMING,
                                "Second programming punchline"
                        ),
                        new Punchline(
                                "punchline-kubernetes-001",
                                "kubernetes-001",
                                JokeCategory.KUBERNETES,
                                "Kubernetes punchline"
                        )
                );

        @Override
        public Optional<Punchline> findBySetupId(
                String setupId
        ) {
            if (setupId == null) {
                return Optional.empty();
            }

            return punchlines.stream()
                    .filter(punchline ->
                            punchline.setupId()
                                    .equalsIgnoreCase(
                                            setupId.trim()
                                    )
                    )
                    .findFirst();
        }

        @Override
        public List<Punchline> findByCategory(
                JokeCategory category
        ) {
            return punchlines.stream()
                    .filter(punchline ->
                            punchline.category() == category
                    )
                    .toList();
        }

        @Override
        public List<Punchline> findAll() {
            return punchlines;
        }
    }
}
