package de.bcpeducation.jokes.audience.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "audience_reactions",
        indexes = {
                @Index(name = "idx_audience_reactions_setup_id", columnList = "setup_id"),
                @Index(name = "idx_audience_reactions_category", columnList = "category"),
                @Index(name = "idx_audience_reactions_reacted_at", columnList = "reacted_at")
        }
)
public class ReactionEntity {

    @Id
    @Column(name = "reaction_id", nullable = false, updatable = false)
    private UUID reactionId;

    @Column(name = "setup_id", nullable = false, length = 255)
    private String setupId;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 50)
    private String reaction;

    @Column(nullable = false)
    private int score;

    @Column(name = "reacted_at", nullable = false)
    private Instant reactedAt;

    @Column(name = "instance_name", length = 255)
    private String instanceName;

    public UUID getReactionId() {
        return reactionId;
    }

    public void setReactionId(UUID reactionId) {
        this.reactionId = reactionId;
    }

    public String getSetupId() {
        return setupId;
    }

    public void setSetupId(String setupId) {
        this.setupId = setupId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Instant getReactedAt() {
        return reactedAt;
    }

    public void setReactedAt(Instant reactedAt) {
        this.reactedAt = reactedAt;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
}
