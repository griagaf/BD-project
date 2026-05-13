CREATE TABLE IF NOT EXISTS app_metadata (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO app_metadata (key, value)
VALUES ('application', 'TACTICAL DISTRICT COMMAND')
ON CONFLICT (key) DO NOTHING;

