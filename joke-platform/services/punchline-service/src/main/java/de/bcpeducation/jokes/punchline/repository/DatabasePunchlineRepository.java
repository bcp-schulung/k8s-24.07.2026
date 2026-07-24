package de.bcpeducation.jokes.punchline.repository;

import de.bcpeducation.jokes.punchline.domain.JokeCategory;
import de.bcpeducation.jokes.punchline.domain.Punchline;
import de.bcpeducation.jokes.punchline.entity.PunchlineEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(
        name = "punchline.repository.provider",
        havingValue = "database",
        matchIfMissing = true
)
public class DatabasePunchlineRepository
        implements PunchlineRepository {

    private final PunchlineJpaRepository jpaRepository;

    public DatabasePunchlineRepository(
            PunchlineJpaRepository jpaRepository
    ) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Punchline> findBySetupId(String setupId) {
        return jpaRepository.findBySetupIdIgnoreCase(setupId)
                .map(this::toDomain);
    }

    @Override
    public List<Punchline> findByCategory(JokeCategory category) {
        return jpaRepository.findByCategory(category.value())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Punchline> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Punchline toDomain(PunchlineEntity entity) {
        return new Punchline(
                entity.getId(),
                entity.getSetupId(),
                JokeCategory.fromValue(entity.getCategory()),
                entity.getText()
        );
    }
}
