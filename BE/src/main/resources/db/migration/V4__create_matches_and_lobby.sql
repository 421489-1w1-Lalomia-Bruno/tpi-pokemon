CREATE TABLE matches (
    id UUID PRIMARY KEY,
    player1_id VARCHAR(100) NOT NULL,
    player2_id VARCHAR(100),
    player1_deck_id UUID,
    player2_deck_id UUID,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    winner_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE match_snapshots (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_snapshot_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    CONSTRAINT uq_snapshot_match UNIQUE (match_id)
);

CREATE INDEX idx_matches_status_created ON matches(status, created_at DESC);
CREATE INDEX idx_matches_player1 ON matches(player1_id);
CREATE INDEX idx_matches_player2 ON matches(player2_id);
