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
