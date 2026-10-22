CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(20) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    gender VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE INDEX idx_users_username ON users(username);
