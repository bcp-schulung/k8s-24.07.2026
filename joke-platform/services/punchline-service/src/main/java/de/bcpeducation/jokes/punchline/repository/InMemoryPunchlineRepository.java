package de.bcpeducation.jokes.punchline.repository;

import de.bcpeducation.jokes.punchline.domain.JokeCategory;
import de.bcpeducation.jokes.punchline.domain.Punchline;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@ConditionalOnProperty(
        name = "punchline.repository.provider",
        havingValue = "memory"
)
public class InMemoryPunchlineRepository
        implements PunchlineRepository {

    private static final List<Punchline> PUNCHLINES = List.of(
            new Punchline(
                    "punchline-programming-001",
                    "programming-001",
                    JokeCategory.PROGRAMMING,
                    "Because they used up all their cache."
            ),
            new Punchline(
                    "punchline-programming-002",
                    "programming-002",
                    JokeCategory.PROGRAMMING,
                    "Because they cannot C#."
            ),
            new Punchline(
                    "punchline-programming-003",
                    "programming-003",
                    JokeCategory.PROGRAMMING,
                    "None. That is a hardware problem."
            ),
            new Punchline(
                    "punchline-programming-004",
                    "programming-004",
                    JokeCategory.PROGRAMMING,
                    "They kept returning null."
            ),

            new Punchline(
                    "punchline-kubernetes-001",
                    "kubernetes-001",
                    JokeCategory.KUBERNETES,
                    "It had too many unresolved container issues."
            ),
            new Punchline(
                    "punchline-kubernetes-002",
                    "kubernetes-002",
                    JokeCategory.KUBERNETES,
                    "It had developed separation anxiety from its sidecar."
            ),
            new Punchline(
                    "punchline-kubernetes-003",
                    "kubernetes-003",
                    JokeCategory.KUBERNETES,
                    "They knew everything would eventually reconcile."
            ),
            new Punchline(
                    "punchline-kubernetes-004",
                    "kubernetes-004",
                    JokeCategory.KUBERNETES,
                    "Do not worry, the Service cannot tell us apart."
            ),

            new Punchline(
                    "punchline-dad-001",
                    "dad-001",
                    JokeCategory.DAD,
                    "It was two-tired."
            ),
            new Punchline(
                    "punchline-dad-002",
                    "dad-002",
                    JokeCategory.DAD,
                    "Nacho cheese."
            ),
            new Punchline(
                    "punchline-dad-003",
                    "dad-003",
                    JokeCategory.DAD,
                    "Because it was outstanding in its field."
            ),
            new Punchline(
                    "punchline-dad-004",
                    "dad-004",
                    JokeCategory.DAD,
                    "In case they get a hole in one."
            ),

            new Punchline(
                    "punchline-animal-001",
                    "animal-001",
                    JokeCategory.ANIMAL,
                    "A gummy bear."
            ),
            new Punchline(
                    "punchline-animal-002",
                    "animal-002",
                    JokeCategory.ANIMAL,
                    "Because their horns do not work."
            ),
            new Punchline(
                    "punchline-animal-003",
                    "animal-003",
                    JokeCategory.ANIMAL,
                    "An investigator."
            ),
            new Punchline(
                    "punchline-animal-004",
                    "animal-004",
                    JokeCategory.ANIMAL,
                    "It wanted to scale across the road."
            ),

            new Punchline(
                    "punchline-science-001",
                    "science-001",
                    JokeCategory.SCIENCE,
                    "Because they make up everything."
            ),
            new Punchline(
                    "punchline-science-002",
                    "science-002",
                    JokeCategory.SCIENCE,
                    "It was traveling light."
            ),
            new Punchline(
                    "punchline-science-003",
                    "science-003",
                    JokeCategory.SCIENCE,
                    "Designer genes."
            ),
            new Punchline(
                    "punchline-science-004",
                    "science-004",
                    JokeCategory.SCIENCE,
                    "It had too many problems."
            )
    );

    @Override
    public Optional<Punchline> findBySetupId(String setupId) {
        if (setupId == null) {
            return Optional.empty();
        }

        String normalizedSetupId =
                setupId.trim().toLowerCase(Locale.ROOT);

        return PUNCHLINES.stream()
                .filter(punchline ->
                        punchline.setupId().equals(normalizedSetupId)
                )
                .findFirst();
    }

    @Override
    public List<Punchline> findByCategory(JokeCategory category) {
        return PUNCHLINES.stream()
                .filter(punchline ->
                        punchline.category() == category
                )
                .toList();
    }

    @Override
    public List<Punchline> findAll() {
        return PUNCHLINES;
    }
}
