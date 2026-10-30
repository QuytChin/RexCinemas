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
