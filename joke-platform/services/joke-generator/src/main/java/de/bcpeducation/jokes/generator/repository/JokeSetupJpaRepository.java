package de.bcpeducation.jokes.generator.repository;

import de.bcpeducation.jokes.generator.entity.JokeSetupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JokeSetupJpaRepository
        extends JpaRepository<JokeSetupEntity, String> {

    List<JokeSetupEntity> findByCategory(String category);
}
