package de.bcpeducation.jokes.generator.repository;

import de.bcpeducation.jokes.generator.domain.JokeCategory;
import de.bcpeducation.jokes.generator.domain.JokeSetup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(
        name = "joke.generator.repository.provider",
        havingValue = "memory"
)
public class InMemoryJokeSetupRepository implements JokeSetupRepository {

    private static final List<JokeSetup> JOKE_SETUPS = List.of(
            new JokeSetup(
                    "programming-001",
                    JokeCategory.PROGRAMMING,
                    "Why did the developer go broke?"
            ),
            new JokeSetup(
                    "programming-002",
                    JokeCategory.PROGRAMMING,
                    "Why do Java developers wear glasses?"
            ),
            new JokeSetup(
                    "programming-003",
                    JokeCategory.PROGRAMMING,
                    "How many programmers does it take to change a light bulb?"
            ),
            new JokeSetup(
                    "programming-004",
                    JokeCategory.PROGRAMMING,
                    "Why did the function stop calling its arguments?"
            ),
            new JokeSetup(
                    "kubernetes-001",
                    JokeCategory.KUBERNETES,
                    "Why did the Kubernetes pod visit a therapist?"
            ),
            new JokeSetup(
                    "kubernetes-002",
                    JokeCategory.KUBERNETES,
                    "Why did the container refuse to leave the cluster?"
            ),
            new JokeSetup(
                    "kubernetes-003",
                    JokeCategory.KUBERNETES,
                    "Why was the Kubernetes administrator so calm during the outage?"
            ),
            new JokeSetup(
                    "kubernetes-004",
                    JokeCategory.KUBERNETES,
                    "What did one replica say to the other replica?"
            ),
            new JokeSetup(
                    "dad-001",
                    JokeCategory.DAD,
                    "Why could the bicycle not stand up by itself?"
            ),
            new JokeSetup(
                    "dad-002",
                    JokeCategory.DAD,
                    "What do you call cheese that does not belong to you?"
            ),
            new JokeSetup(
                    "dad-003",
                    JokeCategory.DAD,
                    "Why did the scarecrow win an award?"
            ),
            new JokeSetup(
                    "dad-004",
                    JokeCategory.DAD,
                    "Why do fathers take an extra pair of socks golfing?"
            ),
            new JokeSetup(
                    "animal-001",
                    JokeCategory.ANIMAL,
                    "What do you call a bear with no teeth?"
            ),
            new JokeSetup(
                    "animal-002",
                    JokeCategory.ANIMAL,
                    "Why do cows wear bells?"
            ),
            new JokeSetup(
                    "animal-003",
                    JokeCategory.ANIMAL,
                    "What do you call an alligator wearing a vest?"
            ),
            new JokeSetup(
                    "animal-004",
                    JokeCategory.ANIMAL,
                    "Why did the chicken join a Kubernetes cluster?"
            ),
            new JokeSetup(
                    "science-001",
                    JokeCategory.SCIENCE,
                    "Why can you never trust an atom?"
            ),
            new JokeSetup(
                    "science-002",
                    JokeCategory.SCIENCE,
                    "Why did the photon refuse to check a suitcase?"
            ),
            new JokeSetup(
                    "science-003",
                    JokeCategory.SCIENCE,
                    "What did the biologist wear to impress the other scientists?"
            ),
            new JokeSetup(
                    "science-004",
                    JokeCategory.SCIENCE,
                    "Why was the math book unhappy?"
            )
    );

    @Override
    public List<JokeSetup> findAll() {
        return JOKE_SETUPS;
    }

    @Override
    public List<JokeSetup> findByCategory(JokeCategory category) {
        return JOKE_SETUPS.stream()
                .filter(jokeSetup -> jokeSetup.category() == category)
                .toList();
    }
}
