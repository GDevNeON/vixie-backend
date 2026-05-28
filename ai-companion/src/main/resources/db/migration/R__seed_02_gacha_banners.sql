-- Repeatable migration: seed gacha banners for UAT/demo
-- Flyway runs this when the checksum changes

-- Clear existing seed data first
DELETE FROM banner_item WHERE banner_id LIKE 'seed_%';
DELETE FROM banner WHERE id LIKE 'seed_%';

-- Standard Banner (always active, all items)
INSERT INTO banner (id, name, description, banner_image_url, start_date, end_date, is_active, pull_cost_one, pull_cost_ten, created_at) VALUES
(
    'seed_standard_banner',
    'Standard Pool',
    'All items available! Try your luck with the standard gacha pool.',
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/standard_banner.jpg',
    NOW() - INTERVAL '30 days',
    NOW() + INTERVAL '365 days',
    TRUE,
    5,
    45,
    NOW()
);

-- Event Banner (limited time, featuring limited item)
INSERT INTO banner (id, name, description, banner_image_url, start_date, end_date, is_active, pull_cost_one, pull_cost_ten, created_at) VALUES
(
    'seed_event_banner',
    'Starlight Echo',
    'Rate up for Epic and Limited items! Featuring Dragon Festival Limited Edition.',
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/starlight_banner.jpg',
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '14 days',
    TRUE,
    5,
    45,
    NOW()
);

-- Standard banner items (all items, equal weight)
INSERT INTO banner_item (id, banner_id, item_id, is_pool_exclusive, drop_rate_weight, created_at) VALUES
('seed_bi_std_01', 'seed_standard_banner', 'seed_sakura_dreams',   FALSE, 1.0, NOW()),
('seed_bi_std_02', 'seed_standard_banner', 'seed_ocean_breeze',    FALSE, 1.0, NOW()),
('seed_bi_std_03', 'seed_standard_banner', 'seed_neon_cyberpunk',  FALSE, 1.0, NOW()),
('seed_bi_std_04', 'seed_standard_banner', 'seed_autumn_forest',   FALSE, 1.0, NOW()),
('seed_bi_std_05', 'seed_standard_banner', 'seed_midnight_galaxy', FALSE, 1.0, NOW());

-- Event banner items (rate-up on epic + limited)
INSERT INTO banner_item (id, banner_id, item_id, is_pool_exclusive, drop_rate_weight, created_at) VALUES
('seed_bi_evt_01', 'seed_event_banner', 'seed_sakura_dreams',   FALSE, 1.0, NOW()),
('seed_bi_evt_02', 'seed_event_banner', 'seed_ocean_breeze',    FALSE, 1.0, NOW()),
('seed_bi_evt_03', 'seed_event_banner', 'seed_neon_cyberpunk',  FALSE, 1.0, NOW()),
('seed_bi_evt_04', 'seed_event_banner', 'seed_autumn_forest',   FALSE, 1.0, NOW()),
('seed_bi_evt_05', 'seed_event_banner', 'seed_midnight_galaxy', FALSE, 2.0, NOW()),  -- Rate up!
('seed_bi_evt_06', 'seed_event_banner', 'seed_dragon_festival', TRUE,  1.5, NOW());  -- Pool exclusive, rate up!
