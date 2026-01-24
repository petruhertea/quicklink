-- V1__Initial_Schema.sql
-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    profile_picture VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    premium BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create user_oauth_providers table
CREATE TABLE IF NOT EXISTS user_oauth_providers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    oauth_provider VARCHAR(50) NOT NULL,
    oauth_id VARCHAR(255) NOT NULL,
    profile_picture VARCHAR(500),
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used TIMESTAMP,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_oauth UNIQUE (oauth_provider, oauth_id)
);

-- Create shortened_url table
CREATE TABLE IF NOT EXISTS shortened_url (
    id BIGSERIAL PRIMARY KEY,
    long_url VARCHAR(2048) NOT NULL,
    short_url VARCHAR(255) UNIQUE NOT NULL,
    code VARCHAR(255) UNIQUE NOT NULL,
    date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    click_count BIGINT NOT NULL DEFAULT 0,
    last_accessed TIMESTAMP,
    user_id BIGINT,
    CONSTRAINT fk_url_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create click_analytics table
CREATE TABLE IF NOT EXISTS click_analytics (
    id BIGSERIAL PRIMARY KEY,
    shortened_url_id BIGINT NOT NULL,
    clicked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    referer VARCHAR(500),
    country VARCHAR(100),
    city VARCHAR(100),
    device_type VARCHAR(50),
    browser VARCHAR(100),
    os VARCHAR(100),
    CONSTRAINT fk_analytics_url FOREIGN KEY (shortened_url_id) REFERENCES shortened_url(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_code ON shortened_url(code);
CREATE INDEX IF NOT EXISTS idx_user_id ON shortened_url(user_id);
CREATE INDEX IF NOT EXISTS idx_expires_at ON shortened_url(expires_at);
CREATE INDEX IF NOT EXISTS idx_shortened_url_id ON click_analytics(shortened_url_id);
CREATE INDEX IF NOT EXISTS idx_clicked_at ON click_analytics(clicked_at);
CREATE INDEX IF NOT EXISTS idx_oauth_provider_id ON user_oauth_providers(oauth_provider, oauth_id);