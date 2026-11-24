CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'VIEWER'
);

-- Apply these additions to the original users table as well as a new database.
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(16) NOT NULL DEFAULT 'VIEWER';
ALTER TABLE users ALTER COLUMN password_hash TYPE TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS users_username_unique ON users (username);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
