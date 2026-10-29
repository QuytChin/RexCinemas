-- RexCinemas MySQL 8.0+ release schema.
-- Generated from the active JPA/native-table model on 2026-10-21.
-- This file creates structure only; it does not contain users, bookings, payments, or demo data.
-- Import into a new database. Existing databases must be backed up and upgraded by an approved procedure.

CREATE DATABASE IF NOT EXISTS `rex_chain_cinema`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `rex_chain_cinema`;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `FKc7q4u7vleq90vlvy8c7lmwtyl` (`showtime_id`),
  CONSTRAINT `FKc7q4u7vleq90vlvy8c7lmwtyl` FOREIGN KEY (`showtime_id`) REFERENCES `showtimes` (`id`),
  CONSTRAINT `FKqferkk98p42soamfn02tqt6sm` FOREIGN KEY (`user_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_settings_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor` varchar(180) NOT NULL,
  `action` varchar(30) NOT NULL,
  `entity_key` varchar(60) NOT NULL,
  `changed_fields` varchar(1000) NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `revoked_access_tokens` (
  `token_hash` varchar(64) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  PRIMARY KEY (`token_hash`),
  KEY `idx_revoked_access_tokens_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
