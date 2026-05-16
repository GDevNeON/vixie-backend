-- V5: Marketplace wallet, transactions, and inventory tables
-- Supports Phase 9: coin wallet with optimistic locking, transaction audit trail, user inventory

CREATE TABLE IF NOT EXISTS coin_wallets (
    user_id      VARCHAR(64)  NOT NULL,
    balance      INTEGER      NOT NULL DEFAULT 0,
    version      INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ,
    CONSTRAINT coin_wallets_pkey PRIMARY KEY (user_id),
    CONSTRAINT fk_coin_wallets_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT coin_wallets_balance_check CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS coin_transactions (
    transaction_id  VARCHAR(64)  NOT NULL,
    user_id         VARCHAR(64)  NOT NULL,
    amount          INTEGER      NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    reason          VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT coin_transactions_pkey PRIMARY KEY (transaction_id),
    CONSTRAINT fk_coin_transactions_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT coin_transactions_type_check CHECK (type IN ('CREDIT', 'DEBIT'))
);

CREATE INDEX idx_coin_transactions_user_id ON coin_transactions(user_id);

CREATE TABLE IF NOT EXISTS user_inventory (
    inventory_id  VARCHAR(64)  NOT NULL,
    user_id       VARCHAR(64)  NOT NULL,
    item_id       VARCHAR(64)  NOT NULL,
    acquired_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT user_inventory_pkey PRIMARY KEY (inventory_id),
    CONSTRAINT fk_user_inventory_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT user_inventory_unique UNIQUE (user_id, item_id)
);

CREATE INDEX idx_user_inventory_user_id ON user_inventory(user_id);
