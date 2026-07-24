package de.bcpeducation.jokes.punchline.repository;

import de.bcpeducation.jokes.punchline.entity.PunchlineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PunchlineJpaRepository
        extends JpaRepository<PunchlineEntity, String> {

    Optional<PunchlineEntity> findBySetupIdIgnoreCase(String setupId);

    List<PunchlineEntity> findByCategory(String category);
}
