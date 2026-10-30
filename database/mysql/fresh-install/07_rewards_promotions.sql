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
