-- RexCinemas MySQL 8 fresh installation. Generated as a standalone file; no SOURCE dependencies.
-- Import only into an empty target database. This script never drops or resets any database.
-- Demo password for seeded demo accounts: 123456. Payment gateway settings are disabled and no external payments are inserted.

-- ===== BEGIN 00_create_database.sql =====
-- RexCinemas clean-install database creation (MySQL 8.0+).
-- This does not drop or reset an existing database.
CREATE DATABASE IF NOT EXISTS `rex_chain_cinema`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `rex_chain_cinema`;
SET NAMES utf8mb4;
SET time_zone = '+07:00';
-- ===== END 00_create_database.sql =====

-- ===== BEGIN 01_schema.sql =====
-- RexCinemas release schema, extracted from database/mysql/schema.sql.
-- JPA entities: 31 tables. Native SQL integration tables: 5 tables.
-- Requires database `rex_chain_cinema` selected by 00_create_database.sql.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `account_notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `deduplication_key` varchar(160) NOT NULL,
  `link` varchar(255) DEFAULT NULL,
  `message` varchar(500) NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `read_at_present` bit(1) NOT NULL,
  `title` varchar(120) NOT NULL,
  `type` varchar(30) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_notification_dedupe` (`user_id`,`deduplication_key`),
  KEY `idx_account_notifications_user_created` (`user_id`,`created_at`),
  CONSTRAINT `FK3glfwp3ro150qvyvp11clrpno` FOREIGN KEY (`user_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `app_users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(300) DEFAULT NULL,
  `avatar_url` varchar(600) DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(150) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `full_name` varchar(120) NOT NULL,
  `lifetime_points` int NOT NULL,
  `loyalty_points` int NOT NULL,
  `national_id` varchar(12) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `role` enum('ADMIN','CINEMA_MANAGER','CUSTOMER','STAFF','SUPERVISOR') NOT NULL,
  `username` varchar(50) DEFAULT NULL,
  `version` bigint NOT NULL,
  `favorite_cinema_id` bigint DEFAULT NULL,
  `cinema_id` bigint DEFAULT NULL,
  `gender` varchar(20) DEFAULT NULL,
  `tier_qualifying_points` int NOT NULL DEFAULT '0',
  `redeemed_points` int NOT NULL DEFAULT '0',
  `reversed_points` int NOT NULL DEFAULT '0',
  `credential_version` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_users_email` (`email`),
  KEY `idx_app_users_favorite_cinema` (`favorite_cinema_id`),
  KEY `idx_app_users_cinema` (`cinema_id`),
  CONSTRAINT `FK6prfudfyeykl21b7q6ivil4hy` FOREIGN KEY (`favorite_cinema_id`) REFERENCES `cinemas` (`id`),
  CONSTRAINT `fk_app_users_cinema` FOREIGN KEY (`cinema_id`) REFERENCES `cinemas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `articles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `category` varchar(80) DEFAULT NULL,
  `content` longtext,
  `created_at` datetime(6) NOT NULL,
  `image_url` varchar(600) DEFAULT NULL,
  `link_url` varchar(600) DEFAULT NULL,
  `publish_date` date DEFAULT NULL,
  `sort_order` int NOT NULL,
  `summary` varchar(500) DEFAULT NULL,
  `title` varchar(220) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `auditoriums` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL DEFAULT b'1',
  `name` varchar(80) NOT NULL,
  `seats_per_row` int NOT NULL,
  `total_rows` int NOT NULL,
  `cinema_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auditoriums_cinema_name` (`cinema_id`,`name`),
  CONSTRAINT `FKhse6clpv2uh59eu9to3ip5hyc` FOREIGN KEY (`cinema_id`) REFERENCES `cinemas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `bank_transfer_snapshots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_code` varchar(30) NOT NULL,
  `bank_name` varchar(120) NOT NULL DEFAULT '',
  `bank_code` varchar(20) NOT NULL,
  `bank_account` varchar(30) NOT NULL,
  `account_holder` varchar(160) NOT NULL DEFAULT '',
  `amount` decimal(12,2) NOT NULL,
  `payment_reference` varchar(120) NOT NULL,
  `expires_at` timestamp(3) NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bank_transfer_snapshot_booking` (`booking_code`),
  UNIQUE KEY `uk_bank_transfer_snapshot_reference` (`payment_reference`),
  KEY `ix_bank_transfer_snapshot_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `booking_combos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `combo_name` varchar(120) NOT NULL,
  `line_total` decimal(12,2) NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `booking_id` bigint NOT NULL,
  `combo_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_booking_combos_booking` (`booking_id`),
  KEY `idx_booking_combos_combo` (`combo_id`),
  CONSTRAINT `FK4u7nced17k5jvgpxbekth5p6l` FOREIGN KEY (`combo_id`) REFERENCES `combo_products` (`id`),
  CONSTRAINT `FKibtryce575stcoiwhwh15l6jf` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `booking_seats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `checked_in` bit(1) NOT NULL,
  `checked_in_at` datetime(6) DEFAULT NULL,
  `checked_in_by` varchar(180) DEFAULT NULL,
  `price` decimal(12,2) NOT NULL,
  `qr_token` varchar(80) DEFAULT NULL,
  `seat_label` varchar(15) NOT NULL,
  `ticket_code` varchar(40) DEFAULT NULL,
  `booking_id` bigint NOT NULL,
  `showtime_seat_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdhjnm23yot8b5ud8l5hklffgc` (`qr_token`),
  KEY `idx_booking_seats_qr_token` (`qr_token`),
  KEY `idx_booking_seats_ticket_code` (`ticket_code`),
  KEY `idx_booking_seats_booking` (`booking_id`),
  KEY `FKk8weh4kajgv9jujbigfp8o0yn` (`showtime_seat_id`),
  CONSTRAINT `FKk8weh4kajgv9jujbigfp8o0yn` FOREIGN KEY (`showtime_seat_id`) REFERENCES `showtime_seats` (`id`),
  CONSTRAINT `FKmbi9ciapn0nvat63t0a8tv478` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_code` varchar(30) NOT NULL,
  `checked_in_at` datetime(6) DEFAULT NULL,
  `checked_in_by` varchar(180) DEFAULT NULL,
  `combo_amount` decimal(12,2) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `discount_amount` decimal(12,2) DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `payment_method` enum('BANK_QR','MOMO_SANDBOX','PAY_AT_COUNTER','VNPAY_SANDBOX') NOT NULL,
  `payment_reference` varchar(120) DEFAULT NULL,
  `points_awarded` bit(1) NOT NULL,
  `qr_token` varchar(80) DEFAULT NULL,
  `refund_note` varchar(255) DEFAULT NULL,
  `refunded_at` datetime(6) DEFAULT NULL,
  `refunded_by` varchar(180) DEFAULT NULL,
  `seat_amount` decimal(12,2) DEFAULT NULL,
  `status` enum('CANCELLED','CONFIRMED','EXPIRED','PENDING','REFUNDED','REFUND_PENDING') NOT NULL,
  `total_amount` decimal(12,2) NOT NULL,
  `version` bigint NOT NULL,
  `voucher_code` varchar(40) DEFAULT NULL,
  `showtime_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `sepay_transaction_id` varchar(80) DEFAULT NULL,
  `earned_points` int NOT NULL DEFAULT '0',
  `points_reversed` tinyint(1) NOT NULL DEFAULT '0',
  `membership_tier_snapshot` varchar(30) DEFAULT NULL,
  `membership_discount_percent` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `membership_discount_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `voucher_discount_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `point_multiplier` decimal(7,4) NOT NULL DEFAULT '1.0000',
  `refund_amount` decimal(12,2) DEFAULT NULL,
  `refund_evidence` varchar(600) DEFAULT NULL,
  `refund_reference` varchar(120) DEFAULT NULL,
  `refunded_by_user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKq97166k18hklq6ls46osbrftx` (`booking_code`),
  UNIQUE KEY `UK7am73665c85ewokrlt864jv84` (`qr_token`),
  UNIQUE KEY `UKggywd6dn1icm3ds2f5aoqhnc2` (`sepay_transaction_id`),
  KEY `idx_bookings_status_created_at` (`status`,`created_at`),
  KEY `idx_bookings_user_created_at` (`user_id`,`created_at`),
  KEY `idx_bookings_payment_reference` (`payment_method`,`payment_reference`),
  KEY `idx_bookings_status_expires_at` (`status`,`expires_at`),
  KEY `FKc7q4u7vleq90vlvy8c7lmwtyl` (`showtime_id`),
  CONSTRAINT `FKc7q4u7vleq90vlvy8c7lmwtyl` FOREIGN KEY (`showtime_id`) REFERENCES `showtimes` (`id`),
  CONSTRAINT `FKqferkk98p42soamfn02tqt6sm` FOREIGN KEY (`user_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cinema_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` longtext,
  `image_url` varchar(600) DEFAULT NULL,
  `label` varchar(80) DEFAULT NULL,
  `link_url` varchar(600) DEFAULT NULL,
  `sort_order` int NOT NULL,
  `title` varchar(180) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cinemas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `address` varchar(255) NOT NULL,
  `city` varchar(100) NOT NULL,
  `name` varchar(120) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cinemas_name_address` (`name`,`address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `combo_products` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `image_url` varchar(600) DEFAULT NULL,
  `name` varchar(120) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `stock_quantity` int NOT NULL,
  `type` enum('COMBO','DRINK','POPCORN') NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `late_payment_case_audits` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(40) NOT NULL,
  `actor` varchar(180) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `detail` varchar(600) DEFAULT NULL,
  `late_payment_case_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_late_payment_audit_case_created` (`late_payment_case_id`,`created_at`),
  CONSTRAINT `FKs1rt9fgav3swcwkdexw1vyhqc` FOREIGN KEY (`late_payment_case_id`) REFERENCES `late_payment_cases` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `late_payment_cases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(12,2) NOT NULL,
  `bank_recorded_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `payment_reference` varchar(120) DEFAULT NULL,
  `provider_transaction_id` varchar(120) NOT NULL,
  `reason` varchar(600) DEFAULT NULL,
  `refund_amount` decimal(12,2) DEFAULT NULL,
  `refund_evidence` varchar(600) DEFAULT NULL,
  `refund_failed_at` datetime(6) DEFAULT NULL,
  `refund_failure_reason` varchar(255) DEFAULT NULL,
  `refund_note` varchar(255) DEFAULT NULL,
  `refund_reference` varchar(120) DEFAULT NULL,
  `refunded_at` datetime(6) DEFAULT NULL,
  `refunded_by` varchar(180) DEFAULT NULL,
  `refunded_by_user_id` bigint DEFAULT NULL,
  `status` enum('NEEDS_REVIEW','PENDING_REFUND','REFUNDED','REFUND_FAILED') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `webhook_received_at` datetime(6) DEFAULT NULL,
  `booking_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_late_payment_provider_transaction` (`provider_transaction_id`),
  KEY `idx_late_payment_status_created` (`status`,`created_at`),
  KEY `idx_late_payment_booking` (`booking_id`),
  CONSTRAINT `FKgjmbyp3amb8plgkko9mu6fdpl` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `loyalty_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `booking_id` bigint DEFAULT NULL,
  `redemption_id` bigint DEFAULT NULL,
  `transaction_type` varchar(40) NOT NULL,
  `source` varchar(40) NOT NULL,
  `reference_key` varchar(120) NOT NULL,
  `points_delta` int NOT NULL,
  `available_before` int NOT NULL,
  `available_after` int NOT NULL,
  `qualifying_delta` int NOT NULL,
  `qualifying_before` int NOT NULL,
  `qualifying_after` int NOT NULL,
  `lifetime_delta` int NOT NULL,
  `lifetime_before` int NOT NULL,
  `lifetime_after` int NOT NULL,
  `redeemed_delta` int NOT NULL,
  `reversed_delta` int NOT NULL,
  `reason` varchar(800) DEFAULT NULL,
  `actor` varchar(180) DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_loyalty_ledger_reference` (`reference_key`),
  KEY `idx_loyalty_ledger_user_created` (`user_id`,`created_at`),
  KEY `idx_loyalty_ledger_type_created` (`transaction_type`,`created_at`),
  KEY `fk_loyalty_ledger_booking` (`booking_id`),
  KEY `fk_loyalty_ledger_redemption` (`redemption_id`),
  CONSTRAINT `fk_loyalty_ledger_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_loyalty_ledger_redemption` FOREIGN KEY (`redemption_id`) REFERENCES `reward_redemptions` (`id`),
  CONSTRAINT `fk_loyalty_ledger_user` FOREIGN KEY (`user_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `loyalty_policy_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `policy_type` varchar(50) NOT NULL,
  `policy_key` varchar(100) NOT NULL,
  `before_value` varchar(4000) DEFAULT NULL,
  `after_value` varchar(4000) DEFAULT NULL,
  `actor` varchar(180) NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_loyalty_policy_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `loyalty_rewards` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint NOT NULL DEFAULT '0',
  `name` varchar(160) NOT NULL,
  `description` varchar(600) DEFAULT NULL,
  `points_cost` int NOT NULL,
  `voucher_amount` decimal(12,2) NOT NULL,
  `voucher_min_order_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `validity_days` int NOT NULL DEFAULT '30',
  `redemption_limit` int DEFAULT NULL,
  `redeemed_count` int NOT NULL DEFAULT '0',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `loyalty_settings` (
  `id` bigint NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `amount_per_point` decimal(12,2) NOT NULL DEFAULT '10000.00',
  `minimum_positive_order_points` int NOT NULL DEFAULT '1',
  `voucher_stacking_allowed` tinyint(1) NOT NULL DEFAULT '0',
  `redemption_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` varchar(180) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `media_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `sort_order` int NOT NULL,
  `thumbnail_url` varchar(600) DEFAULT NULL,
  `title` varchar(180) NOT NULL,
  `type` enum('CLIP','TRAILER') NOT NULL,
  `video_url` varchar(800) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `membership_tier_policies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint NOT NULL DEFAULT '0',
  `tier` varchar(30) NOT NULL,
  `qualifying_threshold` int NOT NULL,
  `ticket_discount_percent` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `point_multiplier` decimal(7,4) NOT NULL DEFAULT '1.0000',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` varchar(180) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_tier_policy_tier` (`tier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `movies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `age_rating` enum('C13','C16','C18','K','P','T13','T16','T18') DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` longtext NOT NULL,
  `duration_minutes` int NOT NULL,
  `genre` varchar(120) DEFAULT NULL,
  `language` varchar(80) DEFAULT NULL,
  `poster_url` varchar(600) DEFAULT NULL,
  `release_date` date DEFAULT NULL,
  `status` enum('COMING_SOON','ENDED','NOW_SHOWING') DEFAULT NULL,
  `title` varchar(180) NOT NULL,
  `trailer_url` varchar(600) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_movies_title_release_date` (`title`,`release_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `otp_challenges` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `birth_date` date DEFAULT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(150) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `failed_attempts` int NOT NULL,
  `favorite_cinema_id` bigint DEFAULT NULL,
  `full_name` varchar(120) NOT NULL,
  `gender` varchar(20) NOT NULL,
  `locked_until` datetime(6) DEFAULT NULL,
  `national_id` varchar(12) DEFAULT NULL,
  `next_send_at` datetime(6) DEFAULT NULL,
  `otp_hash` varchar(64) NOT NULL,
  `otp_salt` varchar(64) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone` varchar(30) NOT NULL,
  `purpose` varchar(40) NOT NULL,
  `terms_accepted` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `username` varchar(50) DEFAULT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_otp_challenge_email_purpose` (`email`,`purpose`),
  KEY `idx_otp_challenge_expires` (`expires_at`),
  KEY `idx_otp_challenge_locked` (`locked_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `otp_rate_limits` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `next_allowed_at` datetime(6) DEFAULT NULL,
  `purpose` varchar(40) NOT NULL,
  `scope_key` varchar(220) NOT NULL,
  `version` bigint NOT NULL,
  `window_count` int NOT NULL,
  `window_started_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_otp_rate_scope_purpose` (`scope_key`,`purpose`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `password_reset_challenges` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_ip` varchar(64) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(150) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `failed_attempts` int NOT NULL,
  `locked_until` datetime(6) DEFAULT NULL,
  `next_send_at` datetime(6) DEFAULT NULL,
  `otp_hash` varchar(64) NOT NULL,
  `otp_salt` varchar(64) NOT NULL,
  `otp_verified` bit(1) NOT NULL,
  `purpose` varchar(40) NOT NULL,
  `reset_token_expires_at` datetime(6) DEFAULT NULL,
  `reset_token_hash` varchar(64) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `verified_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_password_reset_email_purpose` (`email`,`purpose`),
  KEY `idx_password_reset_expires` (`expires_at`),
  KEY `idx_password_reset_token_expires` (`reset_token_expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_method` varchar(30) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '0',
  `bank_name` varchar(120) NOT NULL DEFAULT '',
  `bank_code` varchar(20) NOT NULL DEFAULT '',
  `bank_account` varchar(30) NOT NULL DEFAULT '',
  `account_holder` varchar(160) NOT NULL DEFAULT '',
  `default_account` tinyint(1) NOT NULL DEFAULT '1',
  `payment_timeout_minutes` int NOT NULL DEFAULT '5',
  `payment_prefix` varchar(20) NOT NULL DEFAULT 'REX',
  `payment_description_template` varchar(120) NOT NULL DEFAULT '{BOOKING_CODE}',
  `version` bigint NOT NULL DEFAULT '0',
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_settings_method` (`payment_method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment_settings_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor` varchar(180) NOT NULL,
  `action` varchar(30) NOT NULL,
  `entity_key` varchar(60) NOT NULL,
  `changed_fields` varchar(1000) NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment_transaction_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transaction_id` bigint NOT NULL,
  `action` varchar(40) NOT NULL,
  `actor` varchar(180) NOT NULL,
  `details` varchar(1000) NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `ix_payment_transaction_audit_transaction` (`transaction_id`,`created_at`),
  CONSTRAINT `fk_payment_transaction_audit_transaction` FOREIGN KEY (`transaction_id`) REFERENCES `payment_transactions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider_transaction_id` varchar(120) NOT NULL,
  `booking_code` varchar(30) DEFAULT NULL,
  `payment_reference` varchar(120) DEFAULT NULL,
  `gateway` varchar(120) DEFAULT NULL,
  `transaction_date` varchar(40) DEFAULT NULL,
  `account_number_last4` varchar(4) DEFAULT NULL,
  `code` varchar(120) DEFAULT NULL,
  `content` varchar(500) DEFAULT NULL,
  `transfer_type` varchar(20) DEFAULT NULL,
  `amount` decimal(12,2) DEFAULT NULL,
  `reference_code` varchar(120) DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `received_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `verified_at` timestamp(3) NULL DEFAULT NULL,
  `settled_at` timestamp(3) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_transactions_provider` (`provider_transaction_id`),
  KEY `ix_payment_transactions_status_received` (`status`,`received_at`),
  KEY `ix_payment_transactions_booking` (`booking_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` varchar(1200) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `rating` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `movie_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKovijrmb7g6cvqvlgr25vb8r98` (`user_id`,`movie_id`),
  KEY `FK87tlqya0rq8ijfjscldpvvdyq` (`movie_id`),
  CONSTRAINT `FK87tlqya0rq8ijfjscldpvvdyq` FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`),
  CONSTRAINT `FKfyg8f8n6j63xg66f9mcx94f71` FOREIGN KEY (`user_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `revoked_access_tokens` (
  `token_hash` varchar(64) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  PRIMARY KEY (`token_hash`),
  KEY `idx_revoked_access_tokens_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reward_redemptions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint NOT NULL DEFAULT '0',
  `user_id` bigint NOT NULL,
  `reward_id` bigint NOT NULL,
  `voucher_id` bigint NOT NULL,
  `idempotency_key` varchar(80) NOT NULL,
  `points_cost` int NOT NULL,
  `status` varchar(30) NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `used_at` timestamp(3) NULL DEFAULT NULL,
  `refunded_at` timestamp(3) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reward_redemption_request` (`user_id`,`idempotency_key`),
  UNIQUE KEY `uk_reward_redemption_voucher` (`voucher_id`),
  KEY `idx_reward_redemptions_user_created` (`user_id`,`created_at`),
  KEY `fk_reward_redemptions_reward` (`reward_id`),
  CONSTRAINT `fk_reward_redemptions_reward` FOREIGN KEY (`reward_id`) REFERENCES `loyalty_rewards` (`id`),
  CONSTRAINT `fk_reward_redemptions_user` FOREIGN KEY (`user_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `fk_reward_redemptions_voucher` FOREIGN KEY (`voucher_id`) REFERENCES `vouchers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `seat_hold_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `version` bigint NOT NULL,
  `showtime_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hold_session_user_showtime` (`user_id`,`showtime_id`),
  KEY `idx_hold_session_expires` (`expires_at`),
  KEY `FK78snm2cwqek5lvu4ji0ow4obn` (`showtime_id`),
  CONSTRAINT `FK78snm2cwqek5lvu4ji0ow4obn` FOREIGN KEY (`showtime_id`) REFERENCES `showtimes` (`id`),
  CONSTRAINT `fk_hold_session_user` FOREIGN KEY (`user_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `seats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seat_number` int NOT NULL,
  `seat_row` varchar(5) NOT NULL,
  `type` enum('COUPLE','STANDARD','VIP') NOT NULL,
  `auditorium_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKl7bmfgr721qx6hx1xfg5leeso` (`auditorium_id`,`seat_row`,`seat_number`),
  CONSTRAINT `FK63avm7j8pbf018wn6oacgebp7` FOREIGN KEY (`auditorium_id`) REFERENCES `auditoriums` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `showtime_seats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `held_by_user_id` bigint DEFAULT NULL,
  `hold_until` datetime(6) DEFAULT NULL,
  `price` decimal(12,2) NOT NULL,
  `status` enum('AVAILABLE','BOOKED','HELD') NOT NULL,
  `version` bigint NOT NULL,
  `seat_id` bigint NOT NULL,
  `showtime_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK67piko60j6b7uubu1u84ma1tx` (`showtime_id`,`seat_id`),
  KEY `idx_showtime_seats_showtime_status` (`showtime_id`,`status`),
  KEY `idx_showtime_seats_status_hold_until` (`status`,`hold_until`),
  KEY `FK89iq58er7mpv0dubsii5o48na` (`seat_id`),
  CONSTRAINT `FK89iq58er7mpv0dubsii5o48na` FOREIGN KEY (`seat_id`) REFERENCES `seats` (`id`),
  CONSTRAINT `FKdcy3vgv0vf44n12fq5vouan3b` FOREIGN KEY (`showtime_id`) REFERENCES `showtimes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `showtimes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `base_price` decimal(12,2) NOT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `start_time` datetime(6) NOT NULL,
  `auditorium_id` bigint NOT NULL,
  `movie_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_showtimes_auditorium_time` (`auditorium_id`,`start_time`),
  KEY `idx_showtimes_movie_time` (`movie_id`,`start_time`),
  CONSTRAINT `FKeltpyuei1d5g3n6ikpsjwwil6` FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`),
  CONSTRAINT `FKhaftycg3qn9efpesjctiqv7fw` FOREIGN KEY (`auditorium_id`) REFERENCES `auditoriums` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `vouchers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `code` varchar(40) NOT NULL,
  `discount_type` enum('FIXED','PERCENT') NOT NULL,
  `discount_value` decimal(12,2) NOT NULL,
  `max_discount_amount` decimal(12,2) DEFAULT NULL,
  `min_order_amount` decimal(12,2) DEFAULT NULL,
  `name` varchar(160) NOT NULL,
  `usage_limit` int DEFAULT NULL,
  `used_count` int NOT NULL,
  `valid_from` datetime(6) DEFAULT NULL,
  `valid_until` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `reward_redemption_reference` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK30ftp2biebbvpik8e49wlmady` (`code`),
  KEY `idx_vouchers_owner_user` (`owner_user_id`),
  CONSTRAINT `fk_vouchers_owner_user` FOREIGN KEY (`owner_user_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `support_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `customer_id` bigint DEFAULT NULL,
  `customer_name` varchar(120) DEFAULT NULL,
  `customer_email` varchar(150) DEFAULT NULL,
  `cinema_id` bigint DEFAULT NULL,
  `sender_type` varchar(20) NOT NULL,
  `message` text NOT NULL,
  `is_read` bit(1) NOT NULL DEFAULT b'0',
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_support_session` (`session_id`),
  KEY `idx_support_cinema_created` (`cinema_id`,`created_at`),
  KEY `idx_support_customer` (`customer_id`),
  CONSTRAINT `fk_support_messages_customer` FOREIGN KEY (`customer_id`) REFERENCES `app_users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_support_messages_cinema` FOREIGN KEY (`cinema_id`) REFERENCES `cinemas` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
-- ===== END 01_schema.sql =====

-- ===== BEGIN 02_system_config.sql =====
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
-- ===== END 02_system_config.sql =====

-- ===== BEGIN 03_cinemas_rooms_seats.sql =====
-- Supported Rex Chain sites and room layouts. Seat types mirror CinemaSetupService.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `cinemas` (`id`, `name`, `city`, `address`, `phone`, `active`) VALUES
  (1, 'Rex Chain Quận 1', 'TP. Hồ Chí Minh', '135 Nguyễn Huệ, Phường Bến Nghé, Quận 1', '028 3829 0001', 1),
  (2, 'Rex Chain Gò Vấp', 'TP. Hồ Chí Minh', '18 Phan Văn Trị, Phường 7, Gò Vấp', '028 3895 0002', 1),
  (3, 'Rex Chain Thủ Đức', 'TP. Hồ Chí Minh', '210 Võ Văn Ngân, Phường Bình Thọ, TP. Thủ Đức', '028 3722 0003', 1);

INSERT INTO `auditoriums` (`id`, `cinema_id`, `name`, `total_rows`, `seats_per_row`, `active`) VALUES
  (1, 1, 'Phòng 01 - Laser 4K', 6, 10, 1),
  (2, 1, 'Phòng 02 - Dolby Atmos', 7, 10, 1),
  (3, 2, 'Phòng 01 - Standard', 6, 8, 1),
  (4, 2, 'Phòng 02 - Premium', 6, 10, 1),
  (5, 3, 'Phòng 01 - Laser', 7, 10, 1);

INSERT INTO `seats` (`auditorium_id`, `seat_row`, `seat_number`, `type`)
SELECT a.`id`, CHAR(65 + r.`n`), c.`n`,
       CASE WHEN r.`n` = a.`total_rows` - 1 THEN 'COUPLE'
            WHEN r.`n` >= a.`total_rows` - 3 THEN 'VIP'
            ELSE 'STANDARD' END
FROM `auditoriums` a
JOIN (
  SELECT 0 AS `n` UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
  UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14
  UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19
  UNION ALL SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24
  UNION ALL SELECT 25
) r ON r.`n` < a.`total_rows`
JOIN (
  SELECT 1 AS `n` UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
  UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
  UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
  UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
  UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
  UNION ALL SELECT 31 UNION ALL SELECT 32 UNION ALL SELECT 33 UNION ALL SELECT 34 UNION ALL SELECT 35
  UNION ALL SELECT 36 UNION ALL SELECT 37 UNION ALL SELECT 38 UNION ALL SELECT 39 UNION ALL SELECT 40
  UNION ALL SELECT 41 UNION ALL SELECT 42 UNION ALL SELECT 43 UNION ALL SELECT 44 UNION ALL SELECT 45
  UNION ALL SELECT 46 UNION ALL SELECT 47 UNION ALL SELECT 48 UNION ALL SELECT 49 UNION ALL SELECT 50
) c ON c.`n` <= a.`seats_per_row`
ORDER BY a.`id`, r.`n`, c.`n`;
-- ===== END 03_cinemas_rooms_seats.sql =====

-- ===== BEGIN 04_movies_content.sql =====
-- Vietnamese movie catalog reuses image assets shipped in src/main/resources/static/images.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `movies`
  (`id`, `title`, `description`, `duration_minutes`, `age_rating`, `status`, `genre`, `language`, `release_date`, `poster_url`, `trailer_url`, `active`, `created_at`)
VALUES
  (1, 'Hành Trình Ánh Sáng', 'Một phi hành đoàn trẻ lên đường tìm nguồn năng lượng mới cho Trái Đất giữa hành trình khám phá vũ trụ.', 125, 'C13', 'NOW_SHOWING', 'Khoa học viễn tưởng, Hành động', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 14 DAY), '/images/movies/hanh-trinh-anh-sang.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (2, 'Mùa Hè Không Tên', 'Nhóm bạn thân trải qua mùa hè cuối cùng bên nhau trước ngưỡng cửa trưởng thành.', 112, 'P', 'NOW_SHOWING', 'Tâm lý, Tình cảm', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 8 DAY), '/images/movies/mua-he-khong-ten.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (3, 'Kẻ Gác Đêm', 'Người bảo vệ ca đêm lần theo manh mối của một vụ án bí ẩn tại rạp chiếu bóng cũ.', 118, 'C16', 'NOW_SHOWING', 'Hành động, Bí ẩn', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 4 DAY), '/images/movies/ke-gac-dem.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (4, 'Nhà Có Hai Người', 'Hai thế hệ trong một gia đình học cách thấu hiểu nhau qua những câu chuyện đời thường.', 105, 'P', 'NOW_SHOWING', 'Gia đình, Hài', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 2 DAY), '/images/movies/nha-co-hai-nguoi.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (5, 'Thành Phố Sau Mưa', 'Sau cơn mưa mùa thu, hai tâm hồn cô đơn tình cờ gặp nhau giữa lòng thành phố.', 110, 'C13', 'COMING_SOON', 'Tình cảm, Tâm lý', 'Tiếng Việt', DATE_ADD(CURDATE(), INTERVAL 6 DAY), '/images/movies/thanh-pho-sau-mua.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (6, 'Mật Mã Đại Dương', 'Trạm nghiên cứu biển sâu phát hiện tín hiệu lạ dẫn đến bí mật dưới đáy đại dương.', 132, 'C13', 'COMING_SOON', 'Phiêu lưu, Khoa học viễn tưởng', 'Tiếng Việt', DATE_ADD(CURDATE(), INTERVAL 18 DAY), '/images/movies/mat-ma-dai-duong.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (7, 'Đường Chân Trời Mùa Hạ', 'An trở về miền biển quê nhà và tìm lại ước mơ trong một mùa hè đáng nhớ.', 108, 'P', 'NOW_SHOWING', 'Tình cảm, Gia đình', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 12 DAY), '/images/movies/collection/duong-chan-troi-mua-ha.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (8, 'Bí Mật Ga Cuối', 'Chiếc va-li bỏ quên ở sân ga mở ra bí mật gắn với chuyến tàu đêm.', 116, 'C13', 'NOW_SHOWING', 'Bí ẩn, Tâm lý', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 9 DAY), '/images/movies/collection/bi-mat-ga-cuoi.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (9, 'Bếp Nhà Có Nắng', 'Ba thế hệ trong gia đình tìm thấy niềm vui bên những bữa cơm nhà.', 101, 'P', 'NOW_SHOWING', 'Gia đình, Hài', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 5 DAY), '/images/movies/collection/bep-nha-co-nang.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (10, 'Thành Phố Trong Mưa', 'Một kiến trúc sư trẻ đi tìm ký ức và lựa chọn mới giữa thành phố ngày mưa.', 121, 'C13', 'COMING_SOON', 'Tình cảm, Chính kịch', 'Tiếng Việt', DATE_ADD(CURDATE(), INTERVAL 3 DAY), '/images/movies/collection/thanh-pho-trong-mua.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (11, 'Hành Tinh Thứ Chín', 'Đoàn thám hiểm phát hiện dấu vết lạ trên hành tinh xa ngoài rìa hệ Mặt Trời.', 130, 'C13', 'COMING_SOON', 'Khoa học viễn tưởng, Phiêu lưu', 'Tiếng Việt', DATE_ADD(CURDATE(), INTERVAL 8 DAY), '/images/movies/collection/hanh-tinh-thu-chin.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (12, 'Chuyến Tàu Đêm', 'Những hành khách xa lạ lần theo manh mối về vụ mất tích trên chuyến tàu đêm.', 114, 'C16', 'COMING_SOON', 'Bí ẩn, Giật gân', 'Tiếng Việt', DATE_ADD(CURDATE(), INTERVAL 15 DAY), '/images/movies/collection/chuyen-tau-dem.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (13, 'Vườn Sao Nhỏ', 'Cô bé Linh khám phá khu vườn kỳ diệu chỉ xuất hiện khi màn đêm buông xuống.', 96, 'P', 'ENDED', 'Gia đình, Kỳ ảo', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 120 DAY), '/images/movies/collection/vuon-sao-nho.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (14, 'Ngày Gió Trở Về', 'Trở về quê biển, Huy tìm cách hàn gắn tình thân và bắt đầu lại.', 107, 'P', 'ENDED', 'Chính kịch, Gia đình', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 90 DAY), '/images/movies/collection/ngay-gio-tro-ve.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (15, 'Mật Mã Ánh Trăng', 'Một mật mã cổ dưới đài thiên văn dẫn nữ khảo cổ đến bí ẩn gia đình.', 123, 'C13', 'ENDED', 'Phiêu lưu, Bí ẩn', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 70 DAY), '/images/movies/collection/mat-ma-anh-trang.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (16, 'Tiệm Sách Cuối Phố', 'Hiệu sách nhỏ trở thành nơi những người xa lạ tìm thấy niềm vui và khởi đầu mới.', 99, 'P', 'ENDED', 'Tình cảm, Chính kịch', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 55 DAY), '/images/movies/collection/tien-sach-cuoi-pho.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (17, 'Rừng Thì Thầm', 'Hai người đi rừng lạc vào khu rừng cổ và phát hiện bí mật bị chôn giấu.', 119, 'C16', 'ENDED', 'Phiêu lưu, Kinh dị', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 40 DAY), '/images/movies/collection/rung-thi-tham.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (18, 'Một Ngày Rực Rỡ', 'Một nghệ sĩ đường phố nhận ra niềm vui trong những điều giản dị quanh mình.', 103, 'P', 'ENDED', 'Hài, Tình cảm', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 25 DAY), '/images/movies/collection/mot-ngay-ruc-ro.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW());

INSERT INTO `combo_products` (`id`, `name`, `description`, `price`, `image_url`, `stock_quantity`, `type`, `active`, `version`, `created_at`) VALUES
  (1, 'Combo Solo', '1 bắp rang vừa và 1 nước ngọt', 69000.00, '/images/combos/combo-solo.webp', 499, 'COMBO', 1, 0, NOW()),
  (2, 'Combo Couple', '1 bắp rang lớn và 2 nước ngọt', 119000.00, '/images/combos/combo-couple.webp', 500, 'COMBO', 1, 0, NOW()),
  (3, 'Combo Family', '2 bắp rang lớn và 4 nước ngọt', 219000.00, '/images/combos/combo-family.webp', 300, 'COMBO', 1, 0, NOW()),
  (4, 'Bắp Caramel', 'Bắp caramel cỡ lớn', 59000.00, '/images/combos/bap-caramel.webp', 400, 'POPCORN', 1, 0, NOW());

INSERT INTO `cinema_events` (`id`, `title`, `label`, `description`, `image_url`, `link_url`, `sort_order`, `active`, `created_at`) VALUES
  (1, 'Thứ Tư Thành Viên', 'ƯU ĐÃI THÀNH VIÊN', 'Ưu đãi dành cho hội viên Rex và chương trình tích điểm theo booking hợp lệ.', '/images/events/member-wednesday.webp', '/movies', 1, 1, NOW()),
  (2, 'Cuối tuần xem phim', 'WEEKEND COMBO', 'Gợi ý combo dùng tại rạp cho buổi xem phim cuối tuần.', '/images/events/weekend-combo.webp', '/schedule', 2, 1, NOW());

INSERT INTO `articles` (`id`, `title`, `category`, `summary`, `content`, `image_url`, `link_url`, `publish_date`, `sort_order`, `active`, `created_at`) VALUES
  (1, 'Mẹo chọn ghế xem phim phù hợp', 'TIN ĐIỆN ẢNH', 'Một vài lưu ý về khoảng cách, góc nhìn và âm thanh khi chọn ghế.', 'Sơ đồ ghế hiển thị trạng thái theo thời gian thực. Hãy chọn vị trí phù hợp với số người và trải nghiệm mong muốn trước khi tiếp tục đặt vé.', '/images/articles/chon-ghe-xem-phim.webp', '/schedule', CURDATE(), 1, 1, NOW()),
  (2, 'Tích điểm Rex Rewards', 'REX MEMBER', 'Điểm thưởng và hạng thành viên được ghi nhận theo chính sách trong hệ thống.', 'Điểm đủ điều kiện xét hạng được cộng từ booking hợp lệ. Lịch sử ghi nhận trong ledger để người dùng theo dõi.', '/images/articles/rex-member.webp', '/rewards', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2, 1, NOW()),
  (3, 'Từ lịch chiếu đến ghế ngồi', 'HẬU TRƯỜNG', 'Tìm hiểu cách hệ thống đồng bộ sơ đồ ghế trong một suất chiếu.', 'Seat map được tạo theo phòng chiếu; hệ thống chỉ cho phép một bản ghi ghế trên mỗi suất chiếu.', '/images/articles/booking-realtime.webp', '/schedule', DATE_SUB(CURDATE(), INTERVAL 4 DAY), 3, 1, NOW());

INSERT INTO `media_items` (`id`, `title`, `type`, `thumbnail_url`, `video_url`, `sort_order`, `active`, `created_at`) VALUES
  (1, 'Trailer tuyển chọn Rex', 'TRAILER', '/images/media/trailer-tuyen-chon.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, 1, NOW()),
  (2, 'Không gian Rex Cinemas', 'CLIP', '/images/media/trai-nghiem-rex.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 2, 1, NOW());
-- ===== END 04_movies_content.sql =====

-- ===== BEGIN 05_showtimes.sql =====
-- Rolling schedule: eight calendar days including today, four separated slots per room.
-- End times are derived from movie duration plus a 15-minute room turnover.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `showtimes` (`id`, `movie_id`, `auditorium_id`, `start_time`, `end_time`, `base_price`, `active`)
SELECT 10000 + d.`day_no` * 20 + (a.`id` - 1) * 4 + s.`slot_no` + 1,
       CASE WHEN d.`day_no` = 7 AND a.`id` = 1 AND s.`slot_no` = 0 THEN 5
            ELSE 1 + MOD(d.`day_no` + a.`id` + s.`slot_no` - 1, 4) END,
       a.`id`, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL d.`day_no` DAY), s.`start_clock`),
       ADDTIME(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL d.`day_no` DAY), s.`start_clock`),
               SEC_TO_TIME((m.`duration_minutes` + 15) * 60)),
       CASE WHEN s.`slot_no` < 2 THEN 85000.00 ELSE 95000.00 END,
       1
FROM `auditoriums` a
CROSS JOIN (
  SELECT 0 AS `day_no` UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
  UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) d
CROSS JOIN (
  SELECT 0 AS `slot_no`, '09:30:00' AS `start_clock`
  UNION ALL SELECT 1, '13:30:00'
  UNION ALL SELECT 2, '17:30:00'
  UNION ALL SELECT 3, '21:30:00'
) s
JOIN `movies` m ON m.`id` = CASE WHEN d.`day_no` = 7 AND a.`id` = 1 AND s.`slot_no` = 0 THEN 5
                                ELSE 1 + MOD(d.`day_no` + a.`id` + s.`slot_no` - 1, 4) END;

-- Historic showtimes back the demo booking, check-in, cancelled, expired and refund cases.
INSERT INTO `showtimes` (`id`, `movie_id`, `auditorium_id`, `start_time`, `end_time`, `base_price`, `active`) VALUES
  (1001, 1, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '12:20:00'), 85000.00, 0),
  (1002, 2, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '14:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '16:07:00'), 90000.00, 0),
  (1003, 3, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '18:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '20:13:00'), 85000.00, 0),
  (1004, 4, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '21:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '22:57:00'), 80000.00, 0),
  (1005, 1, 4, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '19:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '21:20:00'), 85000.00, 0);

INSERT INTO `showtime_seats` (`showtime_id`, `seat_id`, `price`, `status`, `version`)
SELECT st.`id`, s.`id`,
       st.`base_price` + CASE s.`type` WHEN 'VIP' THEN 20000.00 WHEN 'COUPLE' THEN 40000.00 ELSE 0.00 END,
       'AVAILABLE', 0
FROM `showtimes` st
JOIN `seats` s ON s.`auditorium_id` = st.`auditorium_id`;
-- ===== END 05_showtimes.sql =====

-- ===== BEGIN 06_demo_users.sql =====
-- Local/demo-only accounts. Password for every row: 123456 (Spring Security BCrypt, cost 10).
-- These credentials are never added to production configuration.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `app_users`
  (`id`, `email`, `full_name`, `username`, `password`, `role`, `cinema_id`, `enabled`,
   `loyalty_points`, `lifetime_points`, `tier_qualifying_points`, `redeemed_points`, `reversed_points`,
   `credential_version`, `version`, `created_at`, `favorite_cinema_id`, `phone`, `gender`, `address`, `avatar_url`, `birth_date`)
VALUES
  (1, 'admin@rexchain.vn', 'Rex Administrator', 'admin', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'ADMIN', NULL, 1, 0, 0, 0, 0, 0, 0, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL),
  (2, 'manager@rexchain.vn', 'Quản lý chuỗi Rex', 'manager', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'CINEMA_MANAGER', NULL, 1, 0, 0, 0, 0, 0, 0, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL),
  (3, 'sup1.q1@rexchain.vn', 'Giám sát Quận 1 - 01', 'sup1.q1', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'SUPERVISOR', 1, 1, 0, 0, 0, 0, 0, 0, 0, NOW(), 1, NULL, NULL, NULL, NULL, NULL),
  (4, 'sup2.q1@rexchain.vn', 'Giám sát Quận 1 - 02', 'sup2.q1', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'SUPERVISOR', 1, 1, 0, 0, 0, 0, 0, 0, 0, NOW(), 1, NULL, NULL, NULL, NULL, NULL),
  (5, 'staff1.q1@rexchain.vn', 'Nhân viên Quận 1', 'staff1.q1', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'STAFF', 1, 1, 0, 0, 0, 0, 0, 0, 0, NOW(), 1, NULL, NULL, NULL, NULL, NULL),
  (6, 'staff2.gv@rexchain.vn', 'Nhân viên Gò Vấp', 'staff2.gv', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'STAFF', 2, 1, 0, 0, 0, 0, 0, 0, 0, NOW(), 2, NULL, NULL, NULL, NULL, NULL),
  (7, 'user@rexchain.vn', 'Khách hàng Rex', 'user', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'CUSTOMER', NULL, 1, 120, 128, 120, 0, 8, 0, 0, NOW(), 1, NULL, NULL, NULL, NULL, NULL),
  (8, 'customer.zero@rexchain.vn', 'Khách hàng mới', 'customer.zero', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'CUSTOMER', NULL, 1, 0, 0, 0, 0, 0, 0, 0, NOW(), 2, NULL, NULL, NULL, NULL, NULL),
  (9, 'customer.nearsilver@rexchain.vn', 'Khách gần lên hạng SILVER', 'customer.nearsilver', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'CUSTOMER', NULL, 1, 249, 249, 249, 0, 0, 0, 0, NOW(), 1, NULL, NULL, NULL, NULL, NULL),
  (10, 'customer.silver@rexchain.vn', 'Khách hàng hạng SILVER', 'customer.silver', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'CUSTOMER', NULL, 1, 200, 250, 250, 50, 0, 0, 0, NOW(), 1, NULL, NULL, NULL, NULL, NULL),
  (11, 'customer.gold@rexchain.vn', 'Khách hàng hạng GOLD', 'customer.gold', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'CUSTOMER', NULL, 1, 600, 600, 600, 0, 0, 0, 0, NOW(), 2, NULL, NULL, NULL, NULL, NULL),
  (12, 'customer.diamond@rexchain.vn', 'Khách hàng hạng DIAMOND', 'customer.diamond', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'CUSTOMER', NULL, 1, 1300, 1300, 1300, 0, 0, 0, 0, NOW(), 3, NULL, NULL, NULL, NULL, NULL),
  (13, 'customer.rewards@rexchain.vn', 'Khách hàng dùng Rex Rewards', 'customer.rewards', '$2a$10$kaaUD6PWosVaT0HZPy37dugdD9ev.nCXZKvE30KtnnbAs3KXPqcu6', 'CUSTOMER', NULL, 1, 220, 320, 320, 100, 0, 0, 0, NOW(), 1, NULL, NULL, NULL, NULL, NULL);
-- ===== END 06_demo_users.sql =====

-- ===== BEGIN 07_rewards_promotions.sql =====
-- Public vouchers and catalog rewards. Reward-owned vouchers are linked in 09_loyalty_redemptions.sql.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `loyalty_rewards`
  (`id`, `version`, `name`, `description`, `points_cost`, `voucher_amount`, `voucher_min_order_amount`,
   `validity_days`, `redemption_limit`, `redeemed_count`, `active`, `created_at`, `updated_at`)
VALUES
  (1, 0, 'Voucher Rex 30.000đ', 'Giảm trực tiếp 30.000đ cho đơn đủ điều kiện.', 100, 30000.00, 100000.00, 30, 500, 1, 1, NOW(), NOW()),
  (2, 0, 'Voucher Rex 50.000đ', 'Ưu đãi đổi điểm dành cho thành viên.', 50, 50000.00, 200000.00, 30, 500, 1, 1, NOW(), NOW()),
  (3, 0, 'Voucher Rex 100.000đ', 'Ưu đãi giá trị cao cho khách hàng tích lũy điểm.', 250, 100000.00, 300000.00, 45, 100, 0, 1, NOW(), NOW());

INSERT INTO `vouchers`
  (`id`, `code`, `name`, `discount_type`, `discount_value`, `min_order_amount`, `max_discount_amount`,
   `valid_from`, `valid_until`, `usage_limit`, `used_count`, `active`, `version`, `owner_user_id`, `reward_redemption_reference`)
VALUES
  (1, 'REX10', 'Rex giảm 10%', 'PERCENT', 10.00, 100000.00, 50000.00, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 180 DAY), 500, 1, 1, 0, NULL, NULL),
  (2, 'REXVIP', 'Rex ưu đãi thành viên', 'PERCENT', 20.00, 200000.00, 80000.00, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 200, 0, 1, 0, NULL, NULL),
  (3, 'REXFIXED', 'Rex giảm 30.000đ', 'FIXED', 30000.00, 150000.00, 30000.00, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), 300, 0, 1, 0, NULL, NULL),
  (4, 'REXOLD', 'Voucher Rex đã hết hạn', 'FIXED', 20000.00, 80000.00, 20000.00, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), 100, 100, 0, 0, NULL, NULL),
  (5, 'RW-DEMO-USED-001', 'Voucher đổi điểm đã dùng', 'FIXED', 30000.00, 100000.00, 30000.00, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), 1, 1, 0, 0, 13, 'REWARD:1'),
  (6, 'RW-DEMO-ISSUED-002', 'Voucher đổi điểm còn hiệu lực', 'FIXED', 50000.00, 200000.00, 50000.00, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 29 DAY), 1, 0, 1, 0, 10, 'REWARD:2');
-- ===== END 07_rewards_promotions.sql =====

-- ===== BEGIN 08_bookings_payments_tickets.sql =====
-- Demonstration booking states. No bank transfer, SePay transaction or real payment credential is seeded.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `bookings`
  (`id`, `booking_code`, `checked_in_at`, `checked_in_by`, `combo_amount`, `created_at`, `discount_amount`,
   `expires_at`, `paid_at`, `payment_method`, `payment_reference`, `points_awarded`, `qr_token`, `refund_note`,
   `refunded_at`, `refunded_by`, `seat_amount`, `status`, `total_amount`, `version`, `voucher_code`, `showtime_id`,
   `user_id`, `sepay_transaction_id`, `earned_points`, `points_reversed`, `membership_tier_snapshot`,
   `membership_discount_percent`, `membership_discount_amount`, `voucher_discount_amount`, `point_multiplier`,
   `refund_amount`, `refund_evidence`, `refund_reference`, `refunded_by_user_id`)
VALUES
  (1, 'REXDEMO0001', NULL, NULL, 0.00, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '08:00:00'), 0.00, NULL,
   TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '08:30:00'), 'PAY_AT_COUNTER', NULL, 1, 'QR-DEMO-BOOKING-0001', NULL,
   NULL, NULL, 85000.00, 'CONFIRMED', 85000.00, 0, NULL, 1001, 7, NULL, 8, 0, 'MEMBER', 0.0000, 0.00, 0.00, 1.0000,
   NULL, NULL, NULL, NULL),
  (2, 'REXDEMO0002', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '17:00:00'), 'staff1.q1@rexchain.vn', 69000.00, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '11:00:00'), 17900.00, NULL,
   TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '12:00:00'), 'PAY_AT_COUNTER', NULL, 1, 'QR-DEMO-BOOKING-0002', NULL,
   NULL, NULL, 110000.00, 'CONFIRMED', 161100.00, 0, 'REX10', 1002, 7, NULL, 16, 0, 'MEMBER', 0.0000, 0.00, 17900.00, 1.0000,
   NULL, NULL, NULL, NULL),
  (3, 'REXDEMO0003', NULL, NULL, 0.00, NOW(), 0.00, DATE_ADD(NOW(), INTERVAL 5 MINUTE),
   NULL, 'BANK_QR', 'DEMO-NO-REAL-TRANSFER', 0, NULL, NULL,
   NULL, NULL, 85000.00, 'PENDING', 85000.00, 0, NULL, 10021, 8, NULL, 0, 0, 'MEMBER', 0.0000, 0.00, 0.00, 1.0000,
   NULL, NULL, NULL, NULL),
  (4, 'REXDEMO0004', NULL, NULL, 0.00, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '16:00:00'), 0.00, NULL,
   NULL, 'BANK_QR', NULL, 0, NULL, NULL,
   NULL, NULL, 85000.00, 'CANCELLED', 85000.00, 0, NULL, 1003, 9, NULL, 0, 0, 'MEMBER', 0.0000, 0.00, 0.00, 1.0000,
   NULL, NULL, NULL, NULL),
  (5, 'REXDEMO0005', NULL, NULL, 0.00, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '18:00:00'), 0.00, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '18:15:00'),
   NULL, 'BANK_QR', 'DEMO-EXPIRED-NO-TRANSFER', 0, NULL, NULL,
   NULL, NULL, 80000.00, 'EXPIRED', 80000.00, 0, NULL, 1004, 8, NULL, 0, 0, 'MEMBER', 0.0000, 0.00, 0.00, 1.0000,
   NULL, NULL, NULL, NULL),
  (6, 'REXDEMO0006', NULL, NULL, 0.00, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '17:00:00'), 0.00, NULL,
   TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:00:00'), 'PAY_AT_COUNTER', NULL, 1, 'QR-DEMO-BOOKING-0006', 'Demo-only refund; no external payment was sent.',
   TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:00:00'), 'admin@rexchain.vn', 85000.00, 'REFUNDED', 85000.00, 0, NULL, 1005, 7, NULL, 8, 1, 'MEMBER', 0.0000, 0.00, 0.00, 1.0000,
   85000.00, 'DEMO ONLY - no bank or gateway transaction', 'DEMO-REFUND-0001', 1);

-- One seat per booking keeps fixtures concise while covering one-seat and combo checkout paths.
INSERT INTO `booking_seats`
  (`checked_in`, `checked_in_at`, `checked_in_by`, `price`, `qr_token`, `seat_label`, `ticket_code`, `booking_id`, `showtime_seat_id`)
SELECT d.`checked_in`, d.`checked_in_at`, d.`checked_in_by`, ss.`price`, d.`qr_token`,
       CONCAT(s.`seat_row`, LPAD(s.`seat_number`, 2, '0')), d.`ticket_code`, b.`id`, ss.`id`
FROM (
  SELECT 1 AS `booking_id`, 'B' AS `seat_row`, 1 AS `seat_number`, 0 AS `checked_in`, NULL AS `checked_in_at`, NULL AS `checked_in_by`, 'QR-DEMO-TICKET-0001' AS `qr_token`, 'TKT-DEMO-0001' AS `ticket_code`
  UNION ALL SELECT 2, 'D', 1, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '17:00:00'), 'staff1.q1@rexchain.vn', 'QR-DEMO-TICKET-0002', 'TKT-DEMO-0002'
  UNION ALL SELECT 3, 'A', 1, 0, NULL, NULL, NULL, NULL
  UNION ALL SELECT 4, 'A', 1, 0, NULL, NULL, NULL, NULL
  UNION ALL SELECT 5, 'A', 1, 0, NULL, NULL, NULL, NULL
  UNION ALL SELECT 6, 'B', 1, 0, NULL, NULL, 'QR-DEMO-TICKET-0006', 'TKT-DEMO-0006'
) d
JOIN `bookings` b ON b.`id` = d.`booking_id`
JOIN `showtimes` st ON st.`id` = b.`showtime_id`
JOIN `seats` s ON s.`auditorium_id` = st.`auditorium_id` AND s.`seat_row` = d.`seat_row` AND s.`seat_number` = d.`seat_number`
JOIN `showtime_seats` ss ON ss.`showtime_id` = st.`id` AND ss.`seat_id` = s.`id`;

INSERT INTO `booking_combos` (`id`, `booking_id`, `combo_id`, `combo_name`, `quantity`, `unit_price`, `line_total`)
VALUES (1, 2, 1, 'Combo Solo', 1, 69000.00, 69000.00);

UPDATE `showtime_seats` ss
JOIN `seats` s ON s.`id` = ss.`seat_id`
SET ss.`status` = 'BOOKED', ss.`held_by_user_id` = NULL, ss.`hold_until` = NULL
WHERE (ss.`showtime_id` = 1001 AND s.`seat_row` = 'B' AND s.`seat_number` = 1)
   OR (ss.`showtime_id` = 1002 AND s.`seat_row` = 'D' AND s.`seat_number` = 1);

-- The only pending checkout is a short-lived local fixture, with no bank snapshot or provider transaction.
UPDATE `showtime_seats` ss
JOIN `seats` s ON s.`id` = ss.`seat_id`
SET ss.`status` = 'HELD', ss.`held_by_user_id` = 8, ss.`hold_until` = DATE_ADD(NOW(), INTERVAL 5 MINUTE)
WHERE ss.`showtime_id` = 10021 AND s.`seat_row` = 'A' AND s.`seat_number` = 1;

INSERT INTO `seat_hold_sessions` (`id`, `created_at`, `expires_at`, `user_id`, `version`, `showtime_id`)
VALUES (1, NOW(), DATE_ADD(NOW(), INTERVAL 5 MINUTE), 8, 0, 10021);
-- ===== END 08_bookings_payments_tickets.sql =====

-- ===== BEGIN 09_loyalty_redemptions.sql =====
-- Seeded loyalty balances are backed by opening, booking and redemption ledger entries.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `reward_redemptions`
  (`id`, `version`, `user_id`, `reward_id`, `voucher_id`, `idempotency_key`, `points_cost`, `status`, `created_at`, `used_at`, `refunded_at`)
VALUES
  (1, 0, 13, 1, 5, 'DEMO-REWARD-USED-0001', 100, 'USED', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NULL),
  (2, 0, 10, 2, 6, 'DEMO-REWARD-ISSUED-0002', 50, 'ISSUED', NOW(), NULL, NULL);

INSERT INTO `loyalty_ledger`
  (`id`, `user_id`, `booking_id`, `redemption_id`, `transaction_type`, `source`, `reference_key`, `points_delta`,
   `available_before`, `available_after`, `qualifying_delta`, `qualifying_before`, `qualifying_after`,
   `lifetime_delta`, `lifetime_before`, `lifetime_after`, `redeemed_delta`, `reversed_delta`, `reason`, `actor`, `created_at`)
VALUES
  (1, 7, NULL, NULL, 'MIGRATION_OPENING', 'MIGRATION', 'DEMO:OPENING:USER7', 96, 0, 96, 96, 0, 96, 96, 0, 96, 0, 0, 'Số dư điểm demo trước các booking minh họa.', 'fresh-install', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (2, 7, 1, NULL, 'EARN', 'BOOKING_PAYMENT', 'EARN:BOOKING:REXDEMO0001', 8, 96, 104, 8, 96, 104, 8, 96, 104, 0, 0, 'Điểm từ booking demo đã thanh toán tại quầy.', 'system', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '08:30:00')),
  (3, 7, 6, NULL, 'EARN', 'BOOKING_PAYMENT', 'EARN:BOOKING:REXDEMO0006', 8, 104, 112, 8, 104, 112, 8, 104, 112, 0, 0, 'Điểm trước khi booking demo được hoàn.', 'system', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:00:00')),
  (4, 7, 2, NULL, 'EARN', 'BOOKING_PAYMENT', 'EARN:BOOKING:REXDEMO0002', 16, 112, 128, 16, 112, 128, 16, 112, 128, 0, 0, 'Điểm từ booking demo sau giảm voucher.', 'system', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '12:00:00')),
  (5, 7, 6, NULL, 'REVERSE_EARN', 'BOOKING_REFUND', 'REVERSE:EARN:BOOKING:REXDEMO0006', -8, 128, 120, -8, 128, 120, 0, 128, 128, 0, 8, 'Thu hồi điểm booking đã hoàn.', 'system', DATE_SUB(NOW(), INTERVAL 4 DAY)),
  (6, 9, NULL, NULL, 'MIGRATION_OPENING', 'MIGRATION', 'DEMO:OPENING:USER9', 249, 0, 249, 249, 0, 249, 249, 0, 249, 0, 0, 'Số dư demo gần ngưỡng SILVER.', 'fresh-install', NOW()),
  (7, 10, NULL, NULL, 'MIGRATION_OPENING', 'MIGRATION', 'DEMO:OPENING:USER10', 250, 0, 250, 250, 0, 250, 250, 0, 250, 0, 0, 'Số dư demo vừa đạt ngưỡng SILVER.', 'fresh-install', NOW()),
  (8, 10, NULL, 2, 'REDEEM', 'REWARD_REDEMPTION', 'REDEEM:10:DEMO-REWARD-ISSUED-0002', -50, 250, 200, 0, 250, 250, 0, 250, 250, 50, 0, 'Đổi điểm lấy voucher demo còn hiệu lực.', 'customer', NOW()),
  (9, 11, NULL, NULL, 'MIGRATION_OPENING', 'MIGRATION', 'DEMO:OPENING:USER11', 600, 0, 600, 600, 0, 600, 600, 0, 600, 0, 0, 'Số dư demo hạng GOLD.', 'fresh-install', NOW()),
  (10, 12, NULL, NULL, 'MIGRATION_OPENING', 'MIGRATION', 'DEMO:OPENING:USER12', 1300, 0, 1300, 1300, 0, 1300, 1300, 0, 1300, 0, 0, 'Số dư demo hạng DIAMOND.', 'fresh-install', NOW()),
  (11, 13, NULL, NULL, 'MIGRATION_OPENING', 'MIGRATION', 'DEMO:OPENING:USER13', 320, 0, 320, 320, 0, 320, 320, 0, 320, 0, 0, 'Số dư demo trước khi đổi voucher.', 'fresh-install', DATE_SUB(NOW(), INTERVAL 20 DAY)),
  (12, 13, NULL, 1, 'REDEEM', 'REWARD_REDEMPTION', 'REDEEM:13:DEMO-REWARD-USED-0001', -100, 320, 220, 0, 320, 320, 0, 320, 320, 100, 0, 'Đổi điểm lấy voucher demo đã sử dụng.', 'customer', DATE_SUB(NOW(), INTERVAL 20 DAY));
-- ===== END 09_loyalty_redemptions.sql =====

-- ===== BEGIN 10_notifications_other.sql =====
-- Notification and support fixtures for the account/admin experience. No mail or push is sent.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `account_notifications`
  (`id`, `user_id`, `title`, `message`, `type`, `link`, `read_at_present`, `read_at`, `deduplication_key`, `created_at`)
VALUES
  (1, 7, 'Chào mừng đến Rex Cinemas', 'Tài khoản demo đã sẵn sàng để khảo sát các chức năng khách hàng.', 'WELCOME', '/movies', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), 'DEMO:WELCOME:USER7', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (2, 7, 'Đặt vé thành công', 'Booking REXDEMO0001 đã được ghi nhận trong dữ liệu minh họa.', 'PAYMENT_CONFIRMED', '/bookings/1', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), 'REXDEMO0001:confirmed', DATE_SUB(NOW(), INTERVAL 7 DAY)),
  (3, 7, 'Thanh toán tại quầy đã xác nhận', 'Booking REXDEMO0002 có vé điện tử và đã check-in trong dữ liệu demo.', 'PAYMENT_CONFIRMED', '/bookings/2', 0, NULL, 'REXDEMO0002:confirmed', DATE_SUB(NOW(), INTERVAL 5 DAY)),
  (4, 7, 'Điểm thưởng được điều chỉnh', 'Điểm của booking REXDEMO0006 đã được thu hồi sau khi ghi nhận hoàn vé demo.', 'LOYALTY_REVERSED', '/rewards', 0, NULL, 'DEMO:REVERSE:REXDEMO0006', DATE_SUB(NOW(), INTERVAL 4 DAY)),
  (5, 8, 'Có booking đang chờ xử lý', 'REXDEMO0003 là fixture chờ hết hạn; không có giao dịch ngân hàng hoặc thanh toán thật.', 'BOOKING_PENDING', '/bookings/3', 0, NULL, 'REXDEMO0003:pending', NOW()),
  (6, 13, 'Voucher Rex Rewards đã sử dụng', 'Voucher đổi điểm trong lịch sử demo đã được đánh dấu sử dụng.', 'REWARD_REDEMPTION', '/rewards', 1, DATE_SUB(NOW(), INTERVAL 14 DAY), 'DEMO:REWARD:USED:USER13', DATE_SUB(NOW(), INTERVAL 15 DAY)),
  (7, 5, 'Nhắc nhở kiểm tra vé tại Quận 1', 'Tài khoản nhân viên demo được gán tại Rex Chain Quận 1.', 'STAFF_NOTICE', '/admin/checkin', 0, NULL, 'DEMO:STAFF:Q1:001', NOW());

INSERT INTO `support_messages`
  (`id`, `session_id`, `customer_id`, `customer_name`, `customer_email`, `cinema_id`, `sender_type`, `message`, `is_read`, `created_at`)
VALUES
  (1, 'demo-support-session-001', 7, 'Khách hàng Rex', 'user@rexchain.vn', 1, 'CUSTOMER', 'Tôi muốn hỏi về cách xem lại vé đã đặt.', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
  (2, 'demo-support-session-001', 7, 'Khách hàng Rex', 'user@rexchain.vn', 1, 'STAFF', 'Bạn có thể đăng nhập rồi mở mục Đặt vé của tôi để xem lịch sử.', 1, DATE_SUB(NOW(), INTERVAL 110 MINUTE)),
  (3, 'demo-support-session-001', 7, 'Khách hàng Rex', 'user@rexchain.vn', 1, 'CUSTOMER', 'Cảm ơn, mình đã tìm thấy rồi.', 0, DATE_SUB(NOW(), INTERVAL 100 MINUTE));

INSERT INTO `reviews` (`id`, `movie_id`, `user_id`, `rating`, `comment`, `created_at`, `updated_at`) VALUES
  (1, 1, 7, 5, 'Hình ảnh đẹp, nhịp phim cuốn hút.', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
  (2, 2, 7, 4, 'Câu chuyện nhẹ nhàng, phù hợp để xem cùng bạn bè.', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));
-- ===== END 10_notifications_other.sql =====

-- ===== BEGIN 11_validation.sql =====
-- Read-only post-install checks. The main database is never reset by this file.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

SELECT DATABASE() AS `database_name`, VERSION() AS `mysql_version`,
       (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE') AS `table_count`,
       (SELECT COUNT(*) FROM information_schema.referential_constraints WHERE constraint_schema = DATABASE()) AS `foreign_key_count`,
       (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND index_name <> 'PRIMARY') AS `secondary_index_count`;

SELECT 'cinemas' AS `dataset`, COUNT(*) AS `rows` FROM `cinemas`
UNION ALL SELECT 'auditoriums', COUNT(*) FROM `auditoriums`
UNION ALL SELECT 'seats', COUNT(*) FROM `seats`
UNION ALL SELECT 'movies', COUNT(*) FROM `movies`
UNION ALL SELECT 'showtimes', COUNT(*) FROM `showtimes`
UNION ALL SELECT 'customers', COUNT(*) FROM `app_users` WHERE `role` = 'CUSTOMER'
UNION ALL SELECT 'bookings', COUNT(*) FROM `bookings`
UNION ALL SELECT 'booking_seats_tickets', COUNT(*) FROM `booking_seats`
UNION ALL SELECT 'payment_transactions', COUNT(*) FROM `payment_transactions`
UNION ALL SELECT 'bank_transfer_snapshots', COUNT(*) FROM `bank_transfer_snapshots`
UNION ALL SELECT 'notifications', COUNT(*) FROM `account_notifications`
UNION ALL SELECT 'vouchers', COUNT(*) FROM `vouchers`
UNION ALL SELECT 'reward_redemptions', COUNT(*) FROM `reward_redemptions`
UNION ALL SELECT 'loyalty_ledger', COUNT(*) FROM `loyalty_ledger`
UNION ALL SELECT 'support_messages', COUNT(*) FROM `support_messages`;

SELECT u.`role`, COUNT(*) AS `accounts`
FROM `app_users` u
GROUP BY u.`role`
ORDER BY u.`role`;

SELECT `tier`, COUNT(*) AS `customers`, MIN(`tier_qualifying_points`) AS `min_qualifying_points`,
       MAX(`tier_qualifying_points`) AS `max_qualifying_points`
FROM (
  SELECT u.`tier_qualifying_points`,
         (SELECT p.`tier` FROM `membership_tier_policies` p
          WHERE p.`active` = 1 AND p.`qualifying_threshold` <= u.`tier_qualifying_points`
          ORDER BY p.`qualifying_threshold` DESC LIMIT 1) AS `tier`
  FROM `app_users` u WHERE u.`role` = 'CUSTOMER'
) member_tiers
GROUP BY `tier` ORDER BY MIN(`tier_qualifying_points`);

SELECT 'orphan_showtime_seat' AS `integrity_check`, COUNT(*) AS `issue_count`
FROM `showtime_seats` ss
LEFT JOIN `showtimes` st ON st.`id` = ss.`showtime_id`
LEFT JOIN `seats` s ON s.`id` = ss.`seat_id`
WHERE st.`id` IS NULL OR s.`id` IS NULL OR s.`auditorium_id` <> st.`auditorium_id`
UNION ALL
SELECT 'booked_seat_without_confirmed_ticket', COUNT(*)
FROM `showtime_seats` ss
LEFT JOIN `booking_seats` bs ON bs.`showtime_seat_id` = ss.`id`
LEFT JOIN `bookings` b ON b.`id` = bs.`booking_id` AND b.`status` = 'CONFIRMED'
WHERE ss.`status` = 'BOOKED' AND b.`id` IS NULL
UNION ALL
SELECT 'confirmed_booking_without_paid_at', COUNT(*)
FROM `bookings` WHERE `status` = 'CONFIRMED' AND `paid_at` IS NULL
UNION ALL
SELECT 'loyalty_balance_not_matching_ledger', COUNT(*)
FROM `app_users` u
WHERE u.`role` = 'CUSTOMER' AND (
  u.`loyalty_points` <> COALESCE((SELECT SUM(l.`points_delta`) FROM `loyalty_ledger` l WHERE l.`user_id` = u.`id`), 0)
  OR u.`tier_qualifying_points` <> COALESCE((SELECT SUM(l.`qualifying_delta`) FROM `loyalty_ledger` l WHERE l.`user_id` = u.`id`), 0)
  OR u.`lifetime_points` <> COALESCE((SELECT SUM(l.`lifetime_delta`) FROM `loyalty_ledger` l WHERE l.`user_id` = u.`id`), 0)
  OR u.`redeemed_points` <> COALESCE((SELECT SUM(l.`redeemed_delta`) FROM `loyalty_ledger` l WHERE l.`user_id` = u.`id`), 0)
  OR u.`reversed_points` <> COALESCE((SELECT SUM(l.`reversed_delta`) FROM `loyalty_ledger` l WHERE l.`user_id` = u.`id`), 0)
);

SELECT `payment_method`, `enabled`, `bank_name`, `bank_code`, `bank_account`
FROM `payment_settings`;

SELECT `provider_transaction_id`, `booking_code`, `payment_reference`, `status`
FROM `payment_transactions`;
-- ===== END 11_validation.sql =====
