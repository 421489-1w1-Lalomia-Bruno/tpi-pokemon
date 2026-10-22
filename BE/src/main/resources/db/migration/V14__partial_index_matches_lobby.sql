DROP INDEX IF EXISTS idx_matches_status_created;
CREATE INDEX idx_matches_lobby_waiting ON matches(created_at DESC) WHERE status = 'WAITING';
