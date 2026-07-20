package de.bcpeducation.jokes.generator.repository;

import de.bcpeducation.jokes.generator.domain.JokeCategory;
import de.bcpeducation.jokes.generator.domain.JokeSetup;

import java.util.List;

public interface JokeSetupRepository {

    List<JokeSetup> findAll();

    List<JokeSetup> findByCategory(JokeCategory category);
}
