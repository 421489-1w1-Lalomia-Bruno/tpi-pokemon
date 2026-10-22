CREATE TABLE game_states (
    match_id UUID PRIMARY KEY,
    current_turn_id VARCHAR(100) NOT NULL,
    game_phase VARCHAR(30) NOT NULL,
    turn_phase VARCHAR(30) NOT NULL,
    board_state_json TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_game_states_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE TABLE game_events (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL,
    player_id VARCHAR(100),
    event_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_game_events_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE INDEX idx_game_events_stream ON game_events(match_id, created_at ASC);
