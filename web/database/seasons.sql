-- ==========================================
-- HyArena2 - Season/League System Tables
-- ==========================================

CREATE TABLE IF NOT EXISTS `seasons` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `slug` VARCHAR(100) NOT NULL UNIQUE,
    `description` TEXT NULL,
    `type` ENUM('system', 'private') NOT NULL DEFAULT 'system',
    `status` ENUM('draft', 'active', 'ended', 'archived') NOT NULL DEFAULT 'draft',
    `starts_at` DATETIME NOT NULL,
    `ends_at` DATETIME NOT NULL,
    `ranking_mode` ENUM('wins', 'win_rate', 'pvp_kills', 'pvp_kd_ratio', 'points') NOT NULL DEFAULT 'wins',
    `ranking_config` JSON NULL COMMENT '{"points_per_win":3,"points_per_loss":1,"points_per_kill":0.5,"points_per_death":0}',
    `min_matches` INT UNSIGNED NOT NULL DEFAULT 5,
    `arena_ids` JSON NULL COMMENT 'NULL = all arenas',
    `game_mode_ids` JSON NULL COMMENT 'NULL = all game modes',
    `visibility` ENUM('public', 'unlisted', 'private') NOT NULL DEFAULT 'public',
    `join_code` VARCHAR(8) NULL UNIQUE,
    `recurrence` ENUM('none','daily','weekly','monthly','yearly') NOT NULL DEFAULT 'none',
    `recurrence_ends_at` DATETIME NULL,
    `base_name` VARCHAR(100) NULL,
    `iteration` INT UNSIGNED NULL,
    `parent_season_id` INT UNSIGNED NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_status` (`status`),
    INDEX `idx_visibility_status` (`visibility`, `status`),
    INDEX `idx_ends_at` (`ends_at`),
    INDEX `idx_recurrence` (`recurrence`),
    FOREIGN KEY (`parent_season_id`) REFERENCES `seasons`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `season_participants` (
    `season_id` INT UNSIGNED NOT NULL,
    `player_uuid` VARCHAR(36) NOT NULL,
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`season_id`, `player_uuid`),
    FOREIGN KEY (`season_id`) REFERENCES `seasons`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`player_uuid`) REFERENCES `players`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `season_player_stats` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `season_id` INT UNSIGNED NOT NULL,
    `player_uuid` VARCHAR(36) NOT NULL,
    `arena_id` VARCHAR(64) NOT NULL,
    `matches_played` INT UNSIGNED NOT NULL DEFAULT 0,
    `matches_won` INT UNSIGNED NOT NULL DEFAULT 0,
    `matches_lost` INT UNSIGNED NOT NULL DEFAULT 0,
    `pvp_kills` INT UNSIGNED NOT NULL DEFAULT 0,
    `pvp_deaths` INT UNSIGNED NOT NULL DEFAULT 0,
    `pve_kills` INT UNSIGNED NOT NULL DEFAULT 0,
    `pve_deaths` INT UNSIGNED NOT NULL DEFAULT 0,
    `damage_dealt` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `damage_taken` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `total_time_played` INT UNSIGNED NOT NULL DEFAULT 0,
    `best_waves_survived` INT UNSIGNED NULL,
    `ranking_points` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `pvp_kd_ratio` DECIMAL(6,2) GENERATED ALWAYS AS (
        CASE WHEN pvp_deaths > 0 THEN ROUND(pvp_kills / pvp_deaths, 2) ELSE pvp_kills END
    ) STORED,
    `win_rate` DECIMAL(5,1) GENERATED ALWAYS AS (
        CASE WHEN matches_played > 0 THEN ROUND(matches_won / matches_played * 100, 1) ELSE 0 END
    ) STORED,
    UNIQUE KEY `uq_season_player_arena` (`season_id`, `player_uuid`, `arena_id`),
    FOREIGN KEY (`season_id`) REFERENCES `seasons`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`player_uuid`) REFERENCES `players`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `season_rankings` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `season_id` INT UNSIGNED NOT NULL,
    `player_uuid` VARCHAR(36) NOT NULL,
    `rank_position` INT UNSIGNED NOT NULL,
    `matches_played` INT UNSIGNED NOT NULL DEFAULT 0,
    `matches_won` INT UNSIGNED NOT NULL DEFAULT 0,
    `matches_lost` INT UNSIGNED NOT NULL DEFAULT 0,
    `pvp_kills` INT UNSIGNED NOT NULL DEFAULT 0,
    `pvp_deaths` INT UNSIGNED NOT NULL DEFAULT 0,
    `pve_kills` INT UNSIGNED NOT NULL DEFAULT 0,
    `pve_deaths` INT UNSIGNED NOT NULL DEFAULT 0,
    `pvp_kd_ratio` DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    `win_rate` DECIMAL(5,1) NOT NULL DEFAULT 0.0,
    `ranking_points` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `ranking_value` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'The actual value used for ranking (depends on ranking_mode)',
    `frozen_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_season_player_rank` (`season_id`, `player_uuid`),
    INDEX `idx_season_rank` (`season_id`, `rank_position`),
    FOREIGN KEY (`season_id`) REFERENCES `seasons`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`player_uuid`) REFERENCES `players`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `season_matches` (
    `season_id` INT UNSIGNED NOT NULL,
    `match_id` BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (`season_id`, `match_id`),
    FOREIGN KEY (`season_id`) REFERENCES `seasons`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`match_id`) REFERENCES `matches`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Aggregated per-player season stats (across all arenas within the season)
CREATE OR REPLACE VIEW `season_player_global_stats` AS
SELECT
    season_id,
    player_uuid,
    SUM(matches_played) AS matches_played,
    SUM(matches_won) AS matches_won,
    SUM(matches_lost) AS matches_lost,
    SUM(pvp_kills) AS pvp_kills,
    SUM(pvp_deaths) AS pvp_deaths,
    SUM(pve_kills) AS pve_kills,
    SUM(pve_deaths) AS pve_deaths,
    SUM(damage_dealt) AS damage_dealt,
    SUM(damage_taken) AS damage_taken,
    SUM(total_time_played) AS total_time_played,
    MAX(best_waves_survived) AS best_waves_survived,
    SUM(ranking_points) AS ranking_points,
    CASE WHEN SUM(pvp_deaths) > 0
         THEN ROUND(SUM(pvp_kills) / SUM(pvp_deaths), 2)
         ELSE SUM(pvp_kills)
    END AS pvp_kd_ratio,
    CASE WHEN SUM(matches_played) > 0
         THEN ROUND(SUM(matches_won) / SUM(matches_played) * 100, 1)
         ELSE 0
    END AS win_rate
FROM season_player_stats
GROUP BY season_id, player_uuid;
