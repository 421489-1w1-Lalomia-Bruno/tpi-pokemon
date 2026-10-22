CREATE TABLE decks (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    creator_id VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE deck_cards (
    id UUID PRIMARY KEY,
    deck_id UUID NOT NULL,
    card_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_deck_card UNIQUE (deck_id, card_id),
    CONSTRAINT fk_deck_cards_deck FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE,
    CONSTRAINT fk_deck_cards_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE RESTRICT
);

CREATE INDEX idx_decks_creator ON decks(creator_id, is_active);
