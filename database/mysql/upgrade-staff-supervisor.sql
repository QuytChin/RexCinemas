-- Run once (or safely rerun) on an EXISTING RexCinemas MySQL 8 database.
-- Back up production data first. This file does not recreate or delete tables.
USE `rex_chain_cinema`;

SET @has_cinema_column = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_users' AND COLUMN_NAME = 'cinema_id'
);
SET @sql = IF(@has_cinema_column = 0,
    'ALTER TABLE `app_users` ADD COLUMN `cinema_id` bigint DEFAULT NULL',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

-- Auditorium archiving: existing rooms remain active after the upgrade.
SET @has_room_active = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'auditoriums' AND COLUMN_NAME = 'active'
);
SET @sql = IF(@has_room_active = 0,
    'ALTER TABLE `auditoriums` ADD COLUMN `active` bit(1) NOT NULL DEFAULT b''1''',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

ALTER TABLE `app_users`
    MODIFY COLUMN `role` enum('ADMIN','CINEMA_MANAGER','CUSTOMER','STAFF','SUPERVISOR') NOT NULL;

SET @has_cinema_index = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_users' AND INDEX_NAME = 'idx_app_users_cinema'
);
SET @sql = IF(@has_cinema_index = 0,
    'CREATE INDEX `idx_app_users_cinema` ON `app_users` (`cinema_id`)',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @has_cinema_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_users'
      AND COLUMN_NAME = 'cinema_id' AND REFERENCED_TABLE_NAME = 'cinemas'
);
SET @sql = IF(@has_cinema_fk = 0,
    'ALTER TABLE `app_users` ADD CONSTRAINT `fk_app_users_cinema` FOREIGN KEY (`cinema_id`) REFERENCES `cinemas` (`id`)',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
