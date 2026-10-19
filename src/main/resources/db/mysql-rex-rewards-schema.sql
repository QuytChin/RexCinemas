-- Rex Rewards schema. Review on a database backup before execution.
-- This file is intentionally NOT registered with spring.sql.init and is not auto-executed.
-- Compatible with MySQL versions before 8.0.29, which do not support ADD COLUMN IF NOT EXISTS.
DROP PROCEDURE IF EXISTS rex_rewards_ensure_column;
DELIMITER //
CREATE PROCEDURE rex_rewards_ensure_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN definition_sql TEXT)
BEGIN
    SET @rex_rewards_sql = (SELECT IF(COUNT(*) = 0,
        CONCAT('ALTER TABLE `', target_table, '` ADD COLUMN `', target_column, '` ', definition_sql),
        'SELECT 1')
        FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = target_table AND column_name = target_column);
    PREPARE rex_rewards_statement FROM @rex_rewards_sql;
    EXECUTE rex_rewards_statement;
    DEALLOCATE PREPARE rex_rewards_statement;
END//
DELIMITER ;

CALL rex_rewards_ensure_column('app_users', 'tier_qualifying_points', 'INT NOT NULL DEFAULT 0');
CALL rex_rewards_ensure_column('app_users', 'redeemed_points', 'INT NOT NULL DEFAULT 0');
CALL rex_rewards_ensure_column('app_users', 'reversed_points', 'INT NOT NULL DEFAULT 0');
CALL rex_rewards_ensure_column('bookings', 'earned_points', 'INT NOT NULL DEFAULT 0');
CALL rex_rewards_ensure_column('bookings', 'points_reversed', 'BOOLEAN NOT NULL DEFAULT FALSE');
CALL rex_rewards_ensure_column('bookings', 'membership_tier_snapshot', 'VARCHAR(30) NULL');
CALL rex_rewards_ensure_column('bookings', 'membership_discount_percent', 'DECIMAL(7,4) NOT NULL DEFAULT 0');
CALL rex_rewards_ensure_column('bookings', 'membership_discount_amount', 'DECIMAL(12,2) NOT NULL DEFAULT 0');
CALL rex_rewards_ensure_column('bookings', 'voucher_discount_amount', 'DECIMAL(12,2) NOT NULL DEFAULT 0');
CALL rex_rewards_ensure_column('bookings', 'point_multiplier', 'DECIMAL(7,4) NOT NULL DEFAULT 1');
CALL rex_rewards_ensure_column('vouchers', 'owner_user_id', 'BIGINT NULL');
CALL rex_rewards_ensure_column('vouchers', 'reward_redemption_reference', 'VARCHAR(80) NULL');
DROP PROCEDURE rex_rewards_ensure_column;

DROP PROCEDURE IF EXISTS rex_rewards_ensure_index;
DELIMITER //
CREATE PROCEDURE rex_rewards_ensure_index(IN target_table VARCHAR(64), IN target_index VARCHAR(64), IN definition_sql TEXT)
BEGIN
    SET @rex_rewards_sql = (SELECT IF(COUNT(*) = 0,
        CONCAT('ALTER TABLE `', target_table, '` ADD INDEX `', target_index, '` ', definition_sql),
        'SELECT 1')
        FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = target_table AND index_name = target_index);
    PREPARE rex_rewards_statement FROM @rex_rewards_sql;
    EXECUTE rex_rewards_statement;
    DEALLOCATE PREPARE rex_rewards_statement;
END//
DELIMITER ;
CALL rex_rewards_ensure_index('vouchers', 'idx_vouchers_owner_user', '(owner_user_id)');
DROP PROCEDURE rex_rewards_ensure_index;

-- MySQL has no ADD CONSTRAINT IF NOT EXISTS. This makes a reviewed re-run safe.
SET @voucher_owner_fk_exists = (SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'vouchers' AND constraint_name = 'fk_vouchers_owner_user');
SET @voucher_owner_fk_sql = IF(@voucher_owner_fk_exists = 0,
    'ALTER TABLE vouchers ADD CONSTRAINT fk_vouchers_owner_user FOREIGN KEY (owner_user_id) REFERENCES app_users(id)',
    'SELECT 1');
PREPARE voucher_owner_fk_stmt FROM @voucher_owner_fk_sql;
EXECUTE voucher_owner_fk_stmt;
DEALLOCATE PREPARE voucher_owner_fk_stmt;

CREATE TABLE IF NOT EXISTS loyalty_settings (
    id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    amount_per_point DECIMAL(12,2) NOT NULL DEFAULT 10000,
    minimum_positive_order_points INT NOT NULL DEFAULT 1,
    voucher_stacking_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    redemption_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(180) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS membership_tier_policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    tier VARCHAR(30) NOT NULL,
    qualifying_threshold INT NOT NULL,
    ticket_discount_percent DECIMAL(7,4) NOT NULL DEFAULT 0,
    point_multiplier DECIMAL(7,4) NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(180) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_tier_policy_tier (tier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS loyalty_rewards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(600) NULL,
    points_cost INT NOT NULL,
    voucher_amount DECIMAL(12,2) NOT NULL,
    voucher_min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    validity_days INT NOT NULL DEFAULT 30,
    redemption_limit INT NULL,
    redeemed_count INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reward_redemptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    reward_id BIGINT NOT NULL,
    voucher_id BIGINT NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    points_cost INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    used_at TIMESTAMP(3) NULL,
    refunded_at TIMESTAMP(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reward_redemption_request (user_id, idempotency_key),
    UNIQUE KEY uk_reward_redemption_voucher (voucher_id),
    KEY idx_reward_redemptions_user_created (user_id, created_at),
    CONSTRAINT fk_reward_redemptions_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_reward_redemptions_reward FOREIGN KEY (reward_id) REFERENCES loyalty_rewards(id),
    CONSTRAINT fk_reward_redemptions_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS loyalty_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    booking_id BIGINT NULL,
    redemption_id BIGINT NULL,
    transaction_type VARCHAR(40) NOT NULL,
    source VARCHAR(40) NOT NULL,
    reference_key VARCHAR(120) NOT NULL,
    points_delta INT NOT NULL,
    available_before INT NOT NULL,
    available_after INT NOT NULL,
    qualifying_delta INT NOT NULL,
    qualifying_before INT NOT NULL,
    qualifying_after INT NOT NULL,
    lifetime_delta INT NOT NULL,
    lifetime_before INT NOT NULL,
    lifetime_after INT NOT NULL,
    redeemed_delta INT NOT NULL,
    reversed_delta INT NOT NULL,
    reason VARCHAR(800) NULL,
    actor VARCHAR(180) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_loyalty_ledger_reference (reference_key),
    KEY idx_loyalty_ledger_user_created (user_id, created_at),
    KEY idx_loyalty_ledger_type_created (transaction_type, created_at),
    CONSTRAINT fk_loyalty_ledger_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_loyalty_ledger_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_loyalty_ledger_redemption FOREIGN KEY (redemption_id) REFERENCES reward_redemptions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS loyalty_policy_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    policy_type VARCHAR(50) NOT NULL,
    policy_key VARCHAR(100) NOT NULL,
    before_value VARCHAR(4000) NULL,
    after_value VARCHAR(4000) NULL,
    actor VARCHAR(180) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_loyalty_policy_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO loyalty_settings (id, version, amount_per_point, minimum_positive_order_points, voucher_stacking_allowed, redemption_enabled, updated_by)
VALUES (1, 0, 10000, 1, FALSE, TRUE, 'migration-default')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO membership_tier_policies (tier, qualifying_threshold, ticket_discount_percent, point_multiplier, active, updated_by)
VALUES ('MEMBER',0,0,1.0,TRUE,'migration-default'),('SILVER',250,3,1.1,TRUE,'migration-default'),('GOLD',600,5,1.2,TRUE,'migration-default'),('DIAMOND',1200,8,1.5,TRUE,'migration-default')
ON DUPLICATE KEY UPDATE tier = VALUES(tier);

INSERT INTO loyalty_rewards (name, description, points_cost, voucher_amount, voucher_min_order_amount, validity_days, active)
SELECT 'Voucher 10.000đ', 'Giảm 10.000đ cho booking hợp lệ', 50, 10000, 0, 30, TRUE WHERE NOT EXISTS (SELECT 1 FROM loyalty_rewards);
INSERT INTO loyalty_rewards (name, description, points_cost, voucher_amount, voucher_min_order_amount, validity_days, active)
SELECT 'Voucher 25.000đ', 'Giảm 25.000đ cho booking hợp lệ', 100, 25000, 0, 30, TRUE WHERE (SELECT COUNT(*) FROM loyalty_rewards) = 1;
INSERT INTO loyalty_rewards (name, description, points_cost, voucher_amount, voucher_min_order_amount, validity_days, active)
SELECT 'Voucher 60.000đ', 'Giảm 60.000đ cho booking hợp lệ', 200, 60000, 0, 30, TRUE WHERE (SELECT COUNT(*) FROM loyalty_rewards) = 2;
INSERT INTO loyalty_rewards (name, description, points_cost, voucher_amount, voucher_min_order_amount, validity_days, active)
SELECT 'Voucher 150.000đ', 'Giảm 150.000đ cho booking hợp lệ', 400, 150000, 0, 30, TRUE WHERE (SELECT COUNT(*) FROM loyalty_rewards) = 3;
