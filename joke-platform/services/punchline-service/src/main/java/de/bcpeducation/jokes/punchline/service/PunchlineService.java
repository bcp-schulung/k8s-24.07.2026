package de.bcpeducation.jokes.punchline.service;

import de.bcpeducation.jokes.punchline.domain.JokeCategory;
import de.bcpeducation.jokes.punchline.domain.Punchline;
import de.bcpeducation.jokes.punchline.error.JokeCategoryNotFoundException;
import de.bcpeducation.jokes.punchline.error.PunchlineNotFoundException;
import de.bcpeducation.jokes.punchline.repository.PunchlineRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

@Service
public class PunchlineService {

    private final PunchlineRepository repository;

    public PunchlineService(PunchlineRepository repository) {
        this.repository = repository;
    }

    public Punchline resolve(String setupId) {
        return repository.findBySetupId(setupId)
                .orElseThrow(() ->
                        new PunchlineNotFoundException(setupId)
                );
    }

    public Punchline selectRandom(
            String categoryValue,
            Long seed
    ) {
        List<Punchline> candidates =
                selectCandidates(categoryValue);

        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                    "No punchlines are configured"
            );
        }

        RandomGenerator randomGenerator =
                createRandomGenerator(seed);

        int selectedIndex =
                randomGenerator.nextInt(candidates.size());

        return candidates.get(selectedIndex);
    }

    private List<Punchline> selectCandidates(
            String categoryValue
    ) {
        if (categoryValue == null ||
                categoryValue.isBlank()) {
            return repository.findAll();
        }

        try {
            JokeCategory category =
                    JokeCategory.fromValue(categoryValue);

            return repository.findByCategory(category);
        } catch (IllegalArgumentException exception) {
            throw new JokeCategoryNotFoundException(
                    categoryValue
            );
        }
    }

    private RandomGenerator createRandomGenerator(Long seed) {
        if (seed == null) {
            return new Random();
        }

        return new Random(seed);
    }
}
