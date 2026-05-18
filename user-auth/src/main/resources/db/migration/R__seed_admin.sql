INSERT INTO users (user_id, created_at, email, password_hash, auth_provider, role)
VALUES ('user_id_admin_2026', NOW(), 'admin@vixie.ai', '$2a$10$l1loDC7FpSYxPLMljqaqhOLrAQBqPoCj.6.LC8pVr.iWdVeiaclr2', 'LOCAL', 'ROLE_ADMIN')
ON CONFLICT (email) DO UPDATE SET role = 'ROLE_ADMIN', password_hash = '$2a$10$l1loDC7FpSYxPLMljqaqhOLrAQBqPoCj.6.LC8pVr.iWdVeiaclr2';

INSERT INTO user_profiles (profile_id, created_at, user_id, username, display_name)
VALUES ('profile_id_admin_2026', NOW(), 'user_id_admin_2026', 'admin', 'System Admin')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO coin_wallets (user_id, balance, version)
VALUES ('user_id_admin_2026', 1000000, 0)
ON CONFLICT (user_id) DO NOTHING;
