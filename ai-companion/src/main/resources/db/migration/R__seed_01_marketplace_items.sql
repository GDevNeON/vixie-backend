-- Repeatable migration: seed marketplace items for UAT/demo
-- Flyway runs this when the checksum changes

-- Clear existing seed data first (repeatable migration)
DELETE FROM marketplace_items WHERE id LIKE 'seed_%';

-- Insert test marketplace items with placeholder Cloudinary URLs
INSERT INTO marketplace_items (id, name, description, rarity, price_coins, price_fiat, thumbnail_url, preview_image_url, status, created_at) VALUES
(
    'seed_sakura_dreams',
    'Sakura Dreams',
    'A serene cherry blossom themed wallpaper pack with animated petals falling gently. Features a calm anime character under a sakura tree.',
    'COMMON',
    50,
    NULL,
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/sakura_dreams_thumb.jpg',
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/sakura_dreams_preview.jpg',
    'PUBLISHED',
    NOW()
),
(
    'seed_neon_cyberpunk',
    'Neon Cyberpunk',
    'A vibrant cyberpunk cityscape with neon lights and rain effects. Character wears futuristic outfit with glowing accents.',
    'RARE',
    100,
    NULL,
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/neon_cyberpunk_thumb.jpg',
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/neon_cyberpunk_preview.jpg',
    'PUBLISHED',
    NOW()
),
(
    'seed_midnight_galaxy',
    'Midnight Galaxy',
    'An enchanting cosmic scene with swirling nebulae and shooting stars. Character floats among the stars with ethereal glow effects.',
    'EPIC',
    200,
    NULL,
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/midnight_galaxy_thumb.jpg',
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/midnight_galaxy_preview.jpg',
    'PUBLISHED',
    NOW()
),
(
    'seed_ocean_breeze',
    'Ocean Breeze',
    'A calming ocean sunset wallpaper with gentle wave animations. Character relaxes on a tropical beach with seashells.',
    'COMMON',
    50,
    NULL,
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/ocean_breeze_thumb.jpg',
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/ocean_breeze_preview.jpg',
    'PUBLISHED',
    NOW()
),
(
    'seed_dragon_festival',
    'Dragon Festival',
    'A spectacular festival scene with dancing dragons and fireworks. Limited edition — character wears traditional festival attire.',
    'LIMITED',
    500,
    NULL,
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/dragon_festival_thumb.jpg',
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/dragon_festival_preview.jpg',
    'PUBLISHED',
    NOW()
),
(
    'seed_autumn_forest',
    'Autumn Forest',
    'A cozy autumn forest with falling leaves and warm lighting. Character sits on a wooden bench reading a book.',
    'RARE',
    100,
    NULL,
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/autumn_forest_thumb.jpg',
    'https://res.cloudinary.com/demo/image/upload/v1/vixie/autumn_forest_preview.jpg',
    'PUBLISHED',
    NOW()
);
