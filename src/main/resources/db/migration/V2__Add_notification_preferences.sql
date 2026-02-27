ALTER TABLE users
    ADD COLUMN notify_expiring_links BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN notification_prompt_shown BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE shortened_url
    ADD COLUMN expiry_notification_sent_at TIMESTAMP;