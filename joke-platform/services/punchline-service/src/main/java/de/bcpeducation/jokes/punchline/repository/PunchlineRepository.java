package de.bcpeducation.jokes.punchline.repository;

import de.bcpeducation.jokes.punchline.domain.JokeCategory;
import de.bcpeducation.jokes.punchline.domain.Punchline;

import java.util.List;
import java.util.Optional;

public interface PunchlineRepository {

    Optional<Punchline> findBySetupId(String setupId);

    List<Punchline> findByCategory(JokeCategory category);

    List<Punchline> findAll();
}
