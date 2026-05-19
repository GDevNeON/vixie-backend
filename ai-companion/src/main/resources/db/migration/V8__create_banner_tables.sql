-- V8: Banner and banner_item tables for Gacha System
-- Supports Phase 10: Banner management with item pool

CREATE TABLE IF NOT EXISTS banner (
    id                VARCHAR(64)   NOT NULL,
    name              VARCHAR(200)  NOT NULL,
    description       TEXT,
    banner_image_url  VARCHAR(2048),
    start_date        TIMESTAMPTZ   NOT NULL,
    end_date          TIMESTAMPTZ   NOT NULL,
    is_active         BOOLEAN       NOT NULL DEFAULT true,
    pull_cost_one     INTEGER       NOT NULL DEFAULT 5,
    pull_cost_ten     INTEGER       NOT NULL DEFAULT 45,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ,
    CONSTRAINT banner_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS banner_item (
    id                  VARCHAR(64)  NOT NULL,
    banner_id           VARCHAR(64)  NOT NULL,
    item_id             VARCHAR(64)  NOT NULL,
    is_pool_exclusive   BOOLEAN      NOT NULL DEFAULT false,
    drop_rate_weight    DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT banner_item_pkey PRIMARY KEY (id),
    CONSTRAINT fk_banner_item_banner FOREIGN KEY (banner_id) REFERENCES banner(id) ON DELETE CASCADE,
    CONSTRAINT fk_banner_item_item FOREIGN KEY (item_id) REFERENCES marketplace_items(id) ON DELETE CASCADE,
    CONSTRAINT banner_item_unique UNIQUE (banner_id, item_id)
);

CREATE INDEX idx_banner_active ON banner(is_active, start_date, end_date);
CREATE INDEX idx_banner_item_banner ON banner_item(banner_id);
