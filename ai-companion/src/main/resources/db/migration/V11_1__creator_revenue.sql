-- Phase 11: Creator revenue tracking
-- Creates creator_sale_records table for per-purchase revenue tracking

CREATE TABLE IF NOT EXISTS creator_sale_records (
    id              VARCHAR(64)     PRIMARY KEY,
    creator_id      VARCHAR(64)     NOT NULL,
    item_id         VARCHAR(64)     NOT NULL,
    gross_coins     INTEGER         NOT NULL,
    creator_coins   INTEGER         NOT NULL,
    platform_coins  INTEGER         NOT NULL,
    purchased_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_sale_records_creator_id ON creator_sale_records(creator_id);
CREATE INDEX IF NOT EXISTS idx_sale_records_item_id ON creator_sale_records(item_id);
