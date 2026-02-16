-- Admin Management Migration
-- Adds bug report resolution tracking and player ban support

-- Add admin notes + resolution tracking to bug reports
ALTER TABLE `bug_reports`
    ADD COLUMN `admin_notes` TEXT DEFAULT NULL AFTER `status`,
    ADD COLUMN `resolved_by` INT UNSIGNED DEFAULT NULL AFTER `admin_notes`,
    ADD COLUMN `resolved_at` TIMESTAMP NULL DEFAULT NULL AFTER `resolved_by`,
    ADD CONSTRAINT `fk_bug_resolved_by` FOREIGN KEY (`resolved_by`) REFERENCES `admin_users`(`id`) ON DELETE SET NULL;

-- Add ban columns to players
ALTER TABLE `players`
    ADD COLUMN `is_banned` TINYINT(1) NOT NULL DEFAULT 0 AFTER `currency`,
    ADD COLUMN `ban_reason` VARCHAR(255) DEFAULT NULL AFTER `is_banned`,
    ADD COLUMN `banned_at` TIMESTAMP NULL DEFAULT NULL AFTER `ban_reason`,
    ADD COLUMN `banned_by` INT UNSIGNED DEFAULT NULL AFTER `banned_at`,
    ADD INDEX `idx_is_banned` (`is_banned`),
    ADD CONSTRAINT `fk_player_banned_by` FOREIGN KEY (`banned_by`) REFERENCES `admin_users`(`id`) ON DELETE SET NULL;
