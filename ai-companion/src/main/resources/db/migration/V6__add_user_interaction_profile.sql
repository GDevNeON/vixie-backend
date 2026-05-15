-- V6: Add user_interaction_profile table for Phase 8 preference learning.
-- Tracks conversation patterns to personalize AI character responses.

CREATE TABLE IF NOT EXISTS user_interaction_profile (
    id                      VARCHAR(64)  PRIMARY KEY,
    user_id                 VARCHAR(64)  NOT NULL UNIQUE,
    avg_message_length      DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    preferred_active_hours  TEXT         NOT NULL DEFAULT '[]',
    top_topics              TEXT         NOT NULL DEFAULT '[]',
    preferred_tone          VARCHAR(32)  NOT NULL DEFAULT 'CASUAL',
    explicit_feedback_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_session_count     INT          NOT NULL DEFAULT 0,
    total_message_count     INT          NOT NULL DEFAULT 0,
    last_session_at         TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_uip_user_id ON user_interaction_profile(user_id);
