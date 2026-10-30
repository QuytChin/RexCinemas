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
