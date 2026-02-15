-- HyArena2 Database Schema v2
-- MySQL/MariaDB

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Kits
CREATE TABLE IF NOT EXISTS `kits` (
    `id` VARCHAR(64) NOT NULL,
    `display_name` VARCHAR(128) NOT NULL,
    `description` TEXT,
    `icon` VARCHAR(128) DEFAULT NULL,
    `is_visible` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Game Modes (synced from plugin)
CREATE TABLE IF NOT EXISTS `game_modes` (
    `id` VARCHAR(64) NOT NULL,
    `display_name` VARCHAR(128) NOT NULL,
    `description` TEXT,
    `is_visible` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Arenas (game_mode as string ID, no FK to arena_types)
CREATE TABLE IF NOT EXISTS `arenas` (
    `id` VARCHAR(64) NOT NULL,
    `display_name` VARCHAR(128) NOT NULL,
    `description` TEXT,
    `game_mode` VARCHAR(64) NOT NULL COMMENT 'duel, lms, deathmatch, koth, kit_roulette',
    `world_name` VARCHAR(128) NOT NULL,
    `min_players` INT UNSIGNED NOT NULL DEFAULT 2,
    `max_players` INT UNSIGNED NOT NULL DEFAULT 2,
    `icon` VARCHAR(128) DEFAULT NULL,
    `is_visible` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_game_mode` (`game_mode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Players
CREATE TABLE IF NOT EXISTS `players` (
    `uuid` CHAR(36) NOT NULL,
    `username` VARCHAR(64) NOT NULL,
    `first_seen` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_seen` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `arena_points` INT NOT NULL DEFAULT 0,
    `honor` INT NOT NULL DEFAULT 0,
    `honor_rank` VARCHAR(32) NOT NULL DEFAULT 'Unranked',
    `currency` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`uuid`),
    UNIQUE INDEX `idx_username` (`username`),
    INDEX `idx_arena_points` (`arena_points` DESC),
    INDEX `idx_honor` (`honor` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Matches
CREATE TABLE IF NOT EXISTS `matches` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `arena_id` VARCHAR(64) NOT NULL,
    `game_mode` VARCHAR(64) NOT NULL,
    `winner_uuid` CHAR(36) DEFAULT NULL,
    `duration_seconds` INT UNSIGNED NOT NULL DEFAULT 0,
    `started_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `ended_at` TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_arena` (`arena_id`),
    INDEX `idx_game_mode` (`game_mode`),
    INDEX `idx_started_at` (`started_at` DESC),
    CONSTRAINT `fk_matches_arena` FOREIGN KEY (`arena_id`) REFERENCES `arenas`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_matches_winner` FOREIGN KEY (`winner_uuid`) REFERENCES `players`(`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Match Participants (unified: players + bots)
CREATE TABLE IF NOT EXISTS `match_participants` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `match_id` BIGINT UNSIGNED NOT NULL,
    `player_uuid` CHAR(36) DEFAULT NULL COMMENT 'NULL for bots',
    `is_bot` TINYINT(1) NOT NULL DEFAULT 0,
    `bot_name` VARCHAR(64) DEFAULT NULL,
    `bot_difficulty` VARCHAR(16) DEFAULT NULL COMMENT 'EASY, MEDIUM, HARD',
    `kit_id` VARCHAR(64) DEFAULT NULL,
    `pvp_kills` INT UNSIGNED NOT NULL DEFAULT 0,
    `pvp_deaths` INT UNSIGNED NOT NULL DEFAULT 0,
    `pve_kills` INT UNSIGNED NOT NULL DEFAULT 0,
    `pve_deaths` INT UNSIGNED NOT NULL DEFAULT 0,
    `damage_dealt` DOUBLE NOT NULL DEFAULT 0,
    `damage_taken` DOUBLE NOT NULL DEFAULT 0,
    `is_winner` TINYINT(1) NOT NULL DEFAULT 0,
    `waves_survived` SMALLINT UNSIGNED DEFAULT NULL COMMENT 'Wave defense: highest wave reached',
    PRIMARY KEY (`id`),
    INDEX `idx_match` (`match_id`),
    INDEX `idx_player` (`player_uuid`),
    INDEX `idx_bot` (`is_bot`),
    CONSTRAINT `fk_participants_match` FOREIGN KEY (`match_id`) REFERENCES `matches`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_participants_player` FOREIGN KEY (`player_uuid`) REFERENCES `players`(`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Player Stats (per-arena aggregates with generated columns)
CREATE TABLE IF NOT EXISTS `player_stats` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `player_uuid` CHAR(36) NOT NULL,
    `arena_id` VARCHAR(64) NOT NULL,
    `matches_played` INT UNSIGNED NOT NULL DEFAULT 0,
    `matches_won` INT UNSIGNED NOT NULL DEFAULT 0,
    `matches_lost` INT UNSIGNED NOT NULL DEFAULT 0,
    `pvp_kills` INT UNSIGNED NOT NULL DEFAULT 0,
    `pvp_deaths` INT UNSIGNED NOT NULL DEFAULT 0,
    `pve_kills` INT UNSIGNED NOT NULL DEFAULT 0,
    `pve_deaths` INT UNSIGNED NOT NULL DEFAULT 0,
    `damage_dealt` DOUBLE NOT NULL DEFAULT 0,
    `damage_taken` DOUBLE NOT NULL DEFAULT 0,
    `total_time_played` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'seconds',
    `best_waves_survived` SMALLINT UNSIGNED DEFAULT NULL COMMENT 'Wave defense: personal best wave',
    `pvp_kd_ratio` DOUBLE GENERATED ALWAYS AS (
        CASE WHEN `pvp_deaths` = 0 THEN `pvp_kills` ELSE ROUND(`pvp_kills` / `pvp_deaths`, 2) END
    ) STORED,
    `win_rate` DOUBLE GENERATED ALWAYS AS (
        CASE WHEN `matches_played` = 0 THEN 0 ELSE ROUND(`matches_won` / `matches_played` * 100, 1) END
    ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_player_arena` (`player_uuid`, `arena_id`),
    INDEX `idx_pvp_kills` (`pvp_kills` DESC),
    INDEX `idx_win_rate` (`win_rate` DESC),
    INDEX `idx_pvp_kd` (`pvp_kd_ratio` DESC),
    CONSTRAINT `fk_stats_player` FOREIGN KEY (`player_uuid`) REFERENCES `players`(`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fk_stats_arena` FOREIGN KEY (`arena_id`) REFERENCES `arenas`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Player Kit Stats (per-kit aggregates)
CREATE TABLE IF NOT EXISTS `player_kit_stats` (
    `player_uuid` CHAR(36) NOT NULL,
    `kit_id` VARCHAR(64) NOT NULL,
    `matches_played` INT UNSIGNED NOT NULL DEFAULT 0,
    `matches_won` INT UNSIGNED NOT NULL DEFAULT 0,
    `pvp_kills` INT UNSIGNED NOT NULL DEFAULT 0,
    `pvp_deaths` INT UNSIGNED NOT NULL DEFAULT 0,
    `pve_kills` INT UNSIGNED NOT NULL DEFAULT 0,
    `pve_deaths` INT UNSIGNED NOT NULL DEFAULT 0,
    `damage_dealt` DOUBLE NOT NULL DEFAULT 0,
    `damage_taken` DOUBLE NOT NULL DEFAULT 0,
    `pvp_kd_ratio` DOUBLE GENERATED ALWAYS AS (
        CASE WHEN `pvp_deaths` = 0 THEN `pvp_kills` ELSE ROUND(`pvp_kills` / `pvp_deaths`, 2) END
    ) STORED,
    `pve_kd_ratio` DOUBLE GENERATED ALWAYS AS (
        CASE WHEN `pve_deaths` = 0 THEN `pve_kills` ELSE ROUND(`pve_kills` / `pve_deaths`, 2) END
    ) STORED,
    `win_rate` DOUBLE GENERATED ALWAYS AS (
        CASE WHEN `matches_played` = 0 THEN 0 ELSE ROUND(`matches_won` / `matches_played` * 100, 1) END
    ) STORED,
    UNIQUE INDEX `idx_player_kit` (`player_uuid`, `kit_id`),
    INDEX `idx_matches_played` (`matches_played` DESC),
    CONSTRAINT `fk_kit_stats_player` FOREIGN KEY (`player_uuid`) REFERENCES `players`(`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fk_kit_stats_kit` FOREIGN KEY (`kit_id`) REFERENCES `kits`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Global stats view (aggregates all per-arena rows per player)
-- Win rate excludes wave_defense arenas (no winner concept)
CREATE OR REPLACE VIEW `player_global_stats` AS
SELECT
    ps.`player_uuid`,
    SUM(ps.`matches_played`) AS `matches_played`,
    SUM(ps.`matches_won`) AS `matches_won`,
    SUM(ps.`matches_lost`) AS `matches_lost`,
    SUM(ps.`pvp_kills`) AS `pvp_kills`,
    SUM(ps.`pvp_deaths`) AS `pvp_deaths`,
    SUM(ps.`pve_kills`) AS `pve_kills`,
    SUM(ps.`pve_deaths`) AS `pve_deaths`,
    SUM(ps.`damage_dealt`) AS `damage_dealt`,
    SUM(ps.`damage_taken`) AS `damage_taken`,
    SUM(ps.`total_time_played`) AS `total_time_played`,
    CASE WHEN SUM(ps.`pvp_deaths`) = 0 THEN SUM(ps.`pvp_kills`)
         ELSE ROUND(SUM(ps.`pvp_kills`) / SUM(ps.`pvp_deaths`), 2) END AS `pvp_kd_ratio`,
    CASE WHEN SUM(CASE WHEN a.`game_mode` != 'wave_defense' THEN ps.`matches_played` ELSE 0 END) = 0 THEN 0
         ELSE ROUND(
            SUM(ps.`matches_won`) /
            SUM(CASE WHEN a.`game_mode` != 'wave_defense' THEN ps.`matches_played` ELSE 0 END) * 100, 1
         ) END AS `win_rate`
FROM `player_stats` ps
JOIN `arenas` a ON ps.`arena_id` = a.`id`
GROUP BY ps.`player_uuid`;

-- Linked Accounts (web registration)
CREATE TABLE IF NOT EXISTS `linked_accounts` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `player_uuid` CHAR(36) DEFAULT NULL COMMENT 'NULL until linked via code',
    `verified_at` TIMESTAMP NULL DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_email` (`email`),
    INDEX `idx_player_uuid` (`player_uuid`),
    CONSTRAINT `fk_linked_player` FOREIGN KEY (`player_uuid`) REFERENCES `players`(`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Link Codes (in-game /link command generates these)
CREATE TABLE IF NOT EXISTS `link_codes` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `code` CHAR(6) NOT NULL,
    `player_uuid` CHAR(36) NOT NULL,
    `expires_at` TIMESTAMP NOT NULL,
    `used` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_code` (`code`),
    INDEX `idx_player_uuid` (`player_uuid`),
    INDEX `idx_expires` (`expires_at`),
    CONSTRAINT `fk_link_code_player` FOREIGN KEY (`player_uuid`) REFERENCES `players`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Admin Users
CREATE TABLE IF NOT EXISTS `admin_users` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` VARCHAR(32) NOT NULL DEFAULT 'admin',
    `last_login` TIMESTAMP NULL DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Admin Login Attempts (brute force protection)
CREATE TABLE IF NOT EXISTS `admin_login_attempts` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `ip_address` VARCHAR(45) NOT NULL,
    `username` VARCHAR(64) DEFAULT NULL,
    `success` TINYINT(1) NOT NULL DEFAULT 0,
    `attempted_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_ip_time` (`ip_address`, `attempted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Server Notifications (shown on website)
CREATE TABLE IF NOT EXISTS `server_notifications` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(255) NOT NULL,
    `message` TEXT NOT NULL,
    `type` VARCHAR(32) NOT NULL DEFAULT 'info' COMMENT 'info, warning, maintenance, update',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `starts_at` TIMESTAMP NULL DEFAULT NULL,
    `ends_at` TIMESTAMP NULL DEFAULT NULL,
    `created_by` INT UNSIGNED DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_active` (`is_active`, `starts_at`, `ends_at`),
    CONSTRAINT `fk_notification_admin` FOREIGN KEY (`created_by`) REFERENCES `admin_users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Discord Webhooks
CREATE TABLE IF NOT EXISTS `discord_webhooks` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(128) NOT NULL,
    `url` VARCHAR(512) NOT NULL,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Webhook Subscriptions (which events a webhook listens to)
CREATE TABLE IF NOT EXISTS `webhook_subscriptions` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `webhook_id` INT UNSIGNED NOT NULL,
    `event_type` VARCHAR(64) NOT NULL COMMENT 'match_end, player_milestone, notification',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_webhook_event` (`webhook_id`, `event_type`),
    CONSTRAINT `fk_sub_webhook` FOREIGN KEY (`webhook_id`) REFERENCES `discord_webhooks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notification Type Defaults (which notification types auto-post to webhooks)
CREATE TABLE IF NOT EXISTS `notification_type_defaults` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `notification_type` VARCHAR(32) NOT NULL,
    `webhook_id` INT UNSIGNED NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_type_webhook` (`notification_type`, `webhook_id`),
    CONSTRAINT `fk_ntd_webhook` FOREIGN KEY (`webhook_id`) REFERENCES `discord_webhooks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notification Webhooks (tracks which notifications were sent to which webhooks)
CREATE TABLE IF NOT EXISTS `notification_webhooks` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `notification_id` BIGINT UNSIGNED NOT NULL,
    `webhook_id` INT UNSIGNED NOT NULL,
    `sent_at` TIMESTAMP NULL DEFAULT NULL,
    `success` TINYINT(1) DEFAULT NULL,
    `response_code` INT DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_notification` (`notification_id`),
    CONSTRAINT `fk_nw_notification` FOREIGN KEY (`notification_id`) REFERENCES `server_notifications`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_nw_webhook` FOREIGN KEY (`webhook_id`) REFERENCES `discord_webhooks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
