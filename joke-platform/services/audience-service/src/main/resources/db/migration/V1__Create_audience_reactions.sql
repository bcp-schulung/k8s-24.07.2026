CREATE TABLE audience_reactions (
    reaction_id   UUID PRIMARY KEY,
    setup_id      VARCHAR(255) NOT NULL,
    category      VARCHAR(50)  NOT NULL,
    reaction      VARCHAR(50)  NOT NULL,
    score         INTEGER      NOT NULL,
    reacted_at    TIMESTAMPTZ  NOT NULL,
    instance_name VARCHAR(255)
);

CREATE INDEX idx_audience_reactions_setup_id  ON audience_reactions(setup_id);
CREATE INDEX idx_audience_reactions_category  ON audience_reactions(category);
CREATE INDEX idx_audience_reactions_reacted_at ON audience_reactions(reacted_at);
