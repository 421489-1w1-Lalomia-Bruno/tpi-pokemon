CREATE TABLE cards (
    id UUID PRIMARY KEY,
    external_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    supertype VARCHAR(30) NOT NULL,
    subtypes JSON,
    hp INTEGER,
    types JSON,
    stage VARCHAR(20),
    evolves_from VARCHAR(100),
    attacks_json TEXT,
    abilities_json TEXT,
    rules TEXT,
    retreat_cost INTEGER,
    weakness_type VARCHAR(30),
    resistance_type VARCHAR(30),
    image_url VARCHAR(500) NOT NULL,
    rarity VARCHAR(50),
    card_number VARCHAR(20) NOT NULL,
    set_id VARCHAR(20) DEFAULT 'xy1' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_cards_search_name ON cards(name);
CREATE INDEX idx_cards_sync_ext ON cards(external_id);
