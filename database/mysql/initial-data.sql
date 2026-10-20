-- Required neutral defaults for a new RexCinemas installation.
-- This file intentionally contains no account, password, booking, payment,
-- voucher, movie, cinema, or demonstration data.
-- Administrator creation remains opt-in through ADMIN_INITIAL_PASSWORD with
-- app.seed.enabled/SEED_ENABLED in a development or controlled environment.

USE `rex_chain_cinema`;

INSERT INTO `loyalty_settings`
    (`id`, `version`, `amount_per_point`, `minimum_positive_order_points`,
     `voucher_stacking_allowed`, `redemption_enabled`, `updated_by`)
VALUES
    (1, 0, 10000.00, 1, 0, 1, 'system')
ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO `membership_tier_policies`
    (`tier`, `qualifying_threshold`, `ticket_discount_percent`, `point_multiplier`, `active`, `updated_by`)
VALUES
    ('MEMBER', 0, 0.0000, 1.0000, 1, 'system'),
    ('SILVER', 250, 3.0000, 1.1000, 1, 'system'),
    ('GOLD', 600, 5.0000, 1.2000, 1, 'system'),
    ('DIAMOND', 1200, 8.0000, 1.5000, 1, 'system')
ON DUPLICATE KEY UPDATE `tier` = `tier`;
