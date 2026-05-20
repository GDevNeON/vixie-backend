-- Phase 11: Creator Tools schema migration
-- Creates creator_profiles table and migrates marketplace_items from is_active to status enum

-- 1. Create creator_profiles table
CREATE TABLE IF NOT EXISTS creator_profiles (
    id              VARCHAR(64)     PRIMARY KEY,
    user_id         VARCHAR(64)     NOT NULL UNIQUE,
    display_name    VARCHAR(100)    NOT NULL,
    bio             TEXT,
    terms_accepted_at TIMESTAMP WITH TIME ZONE,
    is_verified     BOOLEAN         NOT NULL DEFAULT FALSE,
    is_banned       BOOLEAN         NOT NULL DEFAULT FALSE,
    total_sales     INTEGER         NOT NULL DEFAULT 0,
    total_revenue   INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_creator_profiles_user_id ON creator_profiles(user_id);

-- 2. Add creator_id and status to marketplace_items
ALTER TABLE marketplace_items ADD COLUMN IF NOT EXISTS creator_id VARCHAR(64);
ALTER TABLE marketplace_items ADD COLUMN IF NOT EXISTS status VARCHAR(20);

-- 3. Migrate existing data: is_active=true → PUBLISHED, else UNPUBLISHED
UPDATE marketplace_items SET status = 'PUBLISHED' WHERE is_active = TRUE AND status IS NULL;
UPDATE marketplace_items SET status = 'UNPUBLISHED' WHERE is_active = FALSE AND status IS NULL;
UPDATE marketplace_items SET status = 'PUBLISHED' WHERE status IS NULL;

-- 4. Drop the is_active column
ALTER TABLE marketplace_items DROP COLUMN IF EXISTS is_active;

CREATE INDEX IF NOT EXISTS idx_marketplace_items_creator_id ON marketplace_items(creator_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_items_status ON marketplace_items(status);
