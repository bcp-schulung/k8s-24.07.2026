CREATE TABLE punchlines (
    id       VARCHAR(50) PRIMARY KEY,
    setup_id VARCHAR(50)  NOT NULL,
    category VARCHAR(50)  NOT NULL,
    text     VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_punchlines_setup_id ON punchlines(setup_id);
CREATE INDEX idx_punchlines_category ON punchlines(category);
