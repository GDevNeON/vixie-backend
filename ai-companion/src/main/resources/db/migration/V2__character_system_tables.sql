-- V2: Character system tables for Phase 4
-- Creates character definitions, per-user personality overrides, relationship tracking, and user preferences.

-- Character definitions (admin-created only in v1)
CREATE TABLE characters (
    id          VARCHAR(255) NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    avatar_url  VARCHAR(512),
    default_seriousness NUMERIC(3,2) NOT NULL DEFAULT 0.50,
    default_energy      NUMERIC(3,2) NOT NULL DEFAULT 0.50,
    default_gentleness  NUMERIC(3,2) NOT NULL DEFAULT 0.50,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

-- Per-user personality overrides per character (3 sliders)
CREATE TABLE character_personalities (
    id           VARCHAR(255) NOT NULL PRIMARY KEY,
    user_id      VARCHAR(255) NOT NULL,
    character_id VARCHAR(255) NOT NULL REFERENCES characters(id),
    seriousness  NUMERIC(3,2) NOT NULL DEFAULT 0.50,
    energy       NUMERIC(3,2) NOT NULL DEFAULT 0.50,
    gentleness   NUMERIC(3,2) NOT NULL DEFAULT 0.50,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, character_id)
);

-- Relationship state between user and character (XP + Level)
CREATE TABLE relationship_states (
    id              VARCHAR(255) NOT NULL PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL,
    character_id    VARCHAR(255) NOT NULL REFERENCES characters(id),
    level           INTEGER NOT NULL DEFAULT 1,
    current_xp      INTEGER NOT NULL DEFAULT 0,
    xp_to_next_level INTEGER NOT NULL DEFAULT 100,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, character_id)
);

-- User preferences (active companion selection)
CREATE TABLE user_preferences (
    id                  VARCHAR(255) NOT NULL PRIMARY KEY,
    user_id             VARCHAR(255) NOT NULL UNIQUE,
    active_character_id VARCHAR(255) REFERENCES characters(id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);

-- Seed default character: Hana
INSERT INTO characters (id, name, description, avatar_url, default_seriousness, default_energy, default_gentleness, created_at, updated_at)
VALUES (
    'char_hana',
    'Hana',
    'a warm, playful, and caring AI companion who loves chatting about everyday life',
    'https://ui-avatars.com/api/?name=Hana&background=0288D1&color=fff&size=128',
    0.50,
    0.70,
    0.80,
    NOW(),
    NOW()
);
