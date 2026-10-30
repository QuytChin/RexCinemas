-- RexCinemas clean-install database creation (MySQL 8.0+).
-- This does not drop or reset an existing database.
CREATE DATABASE IF NOT EXISTS `rex_chain_cinema`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `rex_chain_cinema`;
SET NAMES utf8mb4;
SET time_zone = '+07:00';
