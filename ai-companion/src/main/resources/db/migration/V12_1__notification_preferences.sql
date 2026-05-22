-- V12_1: Create per-character notification preferences.
CREATE TABLE notification_preferences (
    id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    character_id VARCHAR(255) NOT NULL,
    greeting_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    greeting_time TIME NOT NULL DEFAULT '08:00:00',
    focus_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    focus_time TIME,
    sleep_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    sleep_time TIME,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_preferences_user_character UNIQUE (user_id, character_id)
);

CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);
