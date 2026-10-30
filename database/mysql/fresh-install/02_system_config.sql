-- Neutral system defaults. Payment integrations remain disabled until an operator configures them.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `loyalty_settings`
  (`id`, `version`, `amount_per_point`, `minimum_positive_order_points`, `voucher_stacking_allowed`, `redemption_enabled`, `updated_at`, `updated_by`)
VALUES
  (1, 0, 10000.00, 1, 0, 1, CURRENT_TIMESTAMP(3), 'fresh-install');

INSERT INTO `membership_tier_policies`
  (`id`, `version`, `tier`, `qualifying_threshold`, `ticket_discount_percent`, `point_multiplier`, `active`, `updated_at`, `updated_by`)
VALUES
  (1, 0, 'MEMBER',   0,    0.0000, 1.0000, 1, CURRENT_TIMESTAMP(3), 'fresh-install'),
  (2, 0, 'SILVER', 250,    3.0000, 1.1000, 1, CURRENT_TIMESTAMP(3), 'fresh-install'),
  (3, 0, 'GOLD',   600,    5.0000, 1.2000, 1, CURRENT_TIMESTAMP(3), 'fresh-install'),
  (4, 0, 'DIAMOND',1200,   8.0000, 1.5000, 1, CURRENT_TIMESTAMP(3), 'fresh-install');

INSERT INTO `payment_settings`
  (`payment_method`, `enabled`, `bank_name`, `bank_code`, `bank_account`, `account_holder`, `default_account`,
   `payment_timeout_minutes`, `payment_prefix`, `payment_description_template`, `version`, `updated_at`)
VALUES
  ('BANK_QR', 0, '', '', '', '', 1, 5, 'REX', '{BOOKING_CODE}', 0, CURRENT_TIMESTAMP(3));
