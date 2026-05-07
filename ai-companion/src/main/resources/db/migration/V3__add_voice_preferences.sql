-- V3: Add voice system columns for Phase 7
-- Adds ElevenLabs voice ID to characters and voice preferences to user_preferences.

-- Character voice configuration
ALTER TABLE characters ADD COLUMN elevenlabs_voice_id VARCHAR(255);

-- User voice preferences
ALTER TABLE user_preferences ADD COLUMN voice_muted BOOLEAN DEFAULT FALSE;
ALTER TABLE user_preferences ADD COLUMN voice_volume NUMERIC(3,2) DEFAULT 1.00;
