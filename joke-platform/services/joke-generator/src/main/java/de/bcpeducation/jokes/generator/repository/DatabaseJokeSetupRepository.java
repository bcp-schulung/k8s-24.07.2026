package de.bcpeducation.jokes.generator.repository;

import de.bcpeducation.jokes.generator.domain.JokeCategory;
import de.bcpeducation.jokes.generator.domain.JokeSetup;
import de.bcpeducation.jokes.generator.entity.JokeSetupEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(
        name = "joke.generator.repository.provider",
        havingValue = "database",
        matchIfMissing = true
)
public class DatabaseJokeSetupRepository
        implements JokeSetupRepository {

    private final JokeSetupJpaRepository jpaRepository;

    public DatabaseJokeSetupRepository(
            JokeSetupJpaRepository jpaRepository
    ) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<JokeSetup> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<JokeSetup> findByCategory(JokeCategory category) {
        return jpaRepository.findByCategory(category.value())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private JokeSetup toDomain(JokeSetupEntity entity) {
        return new JokeSetup(
                entity.getId(),
                JokeCategory.fromValue(entity.getCategory()),
                entity.getText()
        );
    }
}
