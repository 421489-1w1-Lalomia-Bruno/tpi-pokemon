CREATE TABLE sync_metadata (
    id UUID PRIMARY KEY,
    set_id VARCHAR(20) NOT NULL UNIQUE,
    last_sync_at TIMESTAMP NOT NULL,
    synced_count INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_sync_metadata_set_id ON sync_metadata(set_id);
