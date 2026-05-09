-- V4: Add voice preference columns to user_preferences table
-- Phase 7: Voice System — supports mute/volume sync across devices

ALTER TABLE user_preferences ADD COLUMN voice_muted BOOLEAN DEFAULT FALSE;
ALTER TABLE user_preferences ADD COLUMN voice_volume DOUBLE PRECISION DEFAULT 1.0;
