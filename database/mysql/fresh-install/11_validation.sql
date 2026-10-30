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
