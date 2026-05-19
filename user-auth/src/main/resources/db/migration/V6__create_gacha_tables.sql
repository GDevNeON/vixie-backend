-- V6: Gacha support tables — pity tracking, daily coin cap, pull records
-- Supports Phase 10: Gacha System

CREATE TABLE IF NOT EXISTS gacha_pity_tracker (
    pity_id       VARCHAR(64)  NOT NULL,
    user_id       VARCHAR(64)  NOT NULL,
    banner_id     VARCHAR(64)  NOT NULL,
    current_pity  INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    CONSTRAINT gacha_pity_tracker_pkey PRIMARY KEY (pity_id),
    CONSTRAINT fk_gacha_pity_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT gacha_pity_unique UNIQUE (user_id, banner_id)
);

CREATE INDEX idx_gacha_pity_user ON gacha_pity_tracker(user_id);

CREATE TABLE IF NOT EXISTS daily_coin_cap (
    cap_id        VARCHAR(64)  NOT NULL,
    user_id       VARCHAR(64)  NOT NULL,
    target_date   DATE         NOT NULL,
    coins_earned  INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    CONSTRAINT daily_coin_cap_pkey PRIMARY KEY (cap_id),
    CONSTRAINT fk_daily_coin_cap_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT daily_coin_cap_unique UNIQUE (user_id, target_date)
);

CREATE INDEX idx_daily_coin_cap_user_date ON daily_coin_cap(user_id, target_date);

CREATE TABLE IF NOT EXISTS gacha_pull_record (
    pull_id       VARCHAR(64)  NOT NULL,
    user_id       VARCHAR(64)  NOT NULL,
    banner_id     VARCHAR(64)  NOT NULL,
    item_id       VARCHAR(64)  NOT NULL,
    rarity        VARCHAR(20)  NOT NULL,
    pull_cost     INTEGER      NOT NULL,
    pulled_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT gacha_pull_record_pkey PRIMARY KEY (pull_id),
    CONSTRAINT fk_gacha_pull_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_gacha_pull_user ON gacha_pull_record(user_id);
CREATE INDEX idx_gacha_pull_banner ON gacha_pull_record(user_id, banner_id);
