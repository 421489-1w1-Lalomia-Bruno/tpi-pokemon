ALTER TABLE cards ALTER COLUMN subtypes TYPE JSONB USING subtypes::jsonb;
ALTER TABLE cards ALTER COLUMN types TYPE JSONB USING types::jsonb;
ALTER TABLE game_states ALTER COLUMN board_state_json TYPE JSONB USING board_state_json::jsonb;
