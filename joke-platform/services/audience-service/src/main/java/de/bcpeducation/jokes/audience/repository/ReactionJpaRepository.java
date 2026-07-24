package de.bcpeducation.jokes.audience.repository;

import de.bcpeducation.jokes.audience.entity.ReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ReactionJpaRepository
        extends JpaRepository<ReactionEntity, UUID> {

    @Query("SELECT COALESCE(SUM(r.score), 0) FROM ReactionEntity r")
    long sumScore();

    @Query("""
            SELECT new de.bcpeducation.jokes.audience.repository.ReactionCount(
                r.reaction, COUNT(r)
            )
            FROM ReactionEntity r
            GROUP BY r.reaction
            """)
    List<ReactionCount> countByReaction();
}
