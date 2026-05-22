-- V12_2: Create user occasion table for recurring special-date notifications.
CREATE TABLE user_occasions (
    id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    label VARCHAR(255) NOT NULL,
    occasion_date VARCHAR(5) NOT NULL,
    notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    detected_from_chat BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_by_user BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_user_occasions_user_id ON user_occasions(user_id);
CREATE INDEX idx_user_occasions_user_date ON user_occasions(user_id, occasion_date);
