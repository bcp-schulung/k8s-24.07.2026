CREATE TABLE joke_setups (
    id       VARCHAR(50) PRIMARY KEY,
    category VARCHAR(50)  NOT NULL,
    text     VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_joke_setups_category ON joke_setups(category);
