package de.bcpeducation.jokes.generator.service;

import de.bcpeducation.jokes.generator.domain.JokeCategory;
import de.bcpeducation.jokes.generator.domain.JokeSetup;
import de.bcpeducation.jokes.generator.error.JokeCategoryNotFoundException;
import de.bcpeducation.jokes.generator.repository.JokeSetupRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

@Service
public class JokeGeneratorService {

    private final JokeSetupRepository repository;

    public JokeGeneratorService(JokeSetupRepository repository) {
        this.repository = repository;
    }

    public JokeSetup generate(String categoryValue, Long seed) {
        List<JokeSetup> candidates = selectCandidates(categoryValue);

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No joke setups are configured");
        }

        RandomGenerator randomGenerator = createRandomGenerator(seed);
        int selectedIndex = randomGenerator.nextInt(candidates.size());

        return candidates.get(selectedIndex);
    }

    public List<JokeCategory> getCategories() {
        return List.of(JokeCategory.values());
    }

    private List<JokeSetup> selectCandidates(String categoryValue) {
        if (categoryValue == null || categoryValue.isBlank()) {
            return repository.findAll();
        }

        try {
            JokeCategory category = JokeCategory.fromValue(categoryValue);
            return repository.findByCategory(category);
        } catch (IllegalArgumentException exception) {
            throw new JokeCategoryNotFoundException(categoryValue);
        }
    }

    private RandomGenerator createRandomGenerator(Long seed) {
        if (seed == null) {
            return RandomGenerator.getDefault();
        }

        return RandomGeneratorFactory
                .<RandomGenerator>of("L64X128MixRandom")
                .create(seed);
    }
}
