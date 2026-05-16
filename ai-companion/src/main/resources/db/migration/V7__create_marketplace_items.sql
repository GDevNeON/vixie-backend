-- V7: Marketplace items catalog table
-- Supports Phase 9: content catalog with rarity, dual pricing, and Cloudinary asset URLs

CREATE TABLE IF NOT EXISTS marketplace_items (
    id                VARCHAR(64)    NOT NULL,
    name              VARCHAR(200)   NOT NULL,
    description       TEXT,
    rarity            VARCHAR(20)    NOT NULL,
    price_coins       INTEGER,
    price_fiat        NUMERIC(10,2),
    thumbnail_url     VARCHAR(2048),
    preview_image_url VARCHAR(2048),
    is_active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ,
    CONSTRAINT marketplace_items_pkey PRIMARY KEY (id),
    CONSTRAINT marketplace_items_rarity_check CHECK (rarity IN ('COMMON', 'RARE', 'EPIC', 'LIMITED'))
);
