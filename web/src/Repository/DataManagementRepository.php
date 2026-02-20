<?php

namespace App\Repository;

use App\Database;
use PDO;

class DataManagementRepository
{
    public function getSummaryCounts(): array
    {
        $db = Database::getConnection();

        $counts = [];
        $counts['total_matches'] = (int) $db->query('SELECT COUNT(*) FROM matches')->fetchColumn();
        $counts['total_participants'] = (int) $db->query('SELECT COUNT(*) FROM match_participants')->fetchColumn();
        $counts['total_player_stats'] = (int) $db->query('SELECT COUNT(*) FROM player_stats')->fetchColumn();
        $counts['total_kit_stats'] = (int) $db->query('SELECT COUNT(*) FROM player_kit_stats')->fetchColumn();
        $counts['orphaned_arenas'] = (int) $db->query('SELECT COUNT(*) FROM arenas WHERE is_visible = 0')->fetchColumn();
        $counts['orphaned_kits'] = (int) $db->query('SELECT COUNT(*) FROM kits WHERE is_visible = 0')->fetchColumn();
        $counts['orphaned_game_modes'] = (int) $db->query('SELECT COUNT(*) FROM game_modes WHERE is_visible = 0')->fetchColumn();

        return $counts;
    }

    public function resetAllStats(): void
    {
        $db = Database::getConnection();
        try {
            $db->exec('SET FOREIGN_KEY_CHECKS = 0');
            $db->exec('TRUNCATE TABLE player_kit_stats');
            $db->exec('TRUNCATE TABLE player_stats');
        } finally {
            $db->exec('SET FOREIGN_KEY_CHECKS = 1');
        }
    }

    public function fullDataPurge(): void
    {
        $db = Database::getConnection();
        try {
            $db->exec('SET FOREIGN_KEY_CHECKS = 0');
            $db->exec('TRUNCATE TABLE match_participants');
            $db->exec('TRUNCATE TABLE player_kit_stats');
            $db->exec('TRUNCATE TABLE player_stats');
            $db->exec('TRUNCATE TABLE matches');
        } finally {
            $db->exec('SET FOREIGN_KEY_CHECKS = 1');
        }
    }

    // --- Orphaned Arenas ---

    public function getOrphanedArenas(): array
    {
        $db = Database::getConnection();
        return $db->query('
            SELECT a.id, a.display_name, a.game_mode,
                (SELECT COUNT(*) FROM matches m WHERE m.arena_id = a.id) AS match_count,
                (SELECT COUNT(*) FROM player_stats ps WHERE ps.arena_id = a.id) AS stats_count
            FROM arenas a
            WHERE a.is_visible = 0
            ORDER BY a.display_name
        ')->fetchAll();
    }

    public function deleteOrphanedArena(string $id): bool
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('DELETE FROM arenas WHERE id = :id AND is_visible = 0');
        $stmt->execute(['id' => $id]);
        return $stmt->rowCount() > 0;
    }

    public function deleteAllOrphanedArenas(): int
    {
        $db = Database::getConnection();
        $stmt = $db->exec('DELETE FROM arenas WHERE is_visible = 0');
        return (int) $stmt;
    }

    // --- Orphaned Kits ---

    public function getOrphanedKits(): array
    {
        $db = Database::getConnection();
        return $db->query('
            SELECT k.id, k.display_name,
                (SELECT COUNT(*) FROM player_kit_stats pks WHERE pks.kit_id = k.id) AS kit_stats_count
            FROM kits k
            WHERE k.is_visible = 0
            ORDER BY k.display_name
        ')->fetchAll();
    }

    public function deleteOrphanedKit(string $id): bool
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('DELETE FROM kits WHERE id = :id AND is_visible = 0');
        $stmt->execute(['id' => $id]);
        return $stmt->rowCount() > 0;
    }

    public function deleteAllOrphanedKits(): int
    {
        $db = Database::getConnection();
        $stmt = $db->exec('DELETE FROM kits WHERE is_visible = 0');
        return (int) $stmt;
    }

    // --- Orphaned Game Modes ---

    public function getOrphanedGameModes(): array
    {
        $db = Database::getConnection();
        return $db->query('
            SELECT gm.id, gm.display_name,
                (SELECT COUNT(*) FROM matches m WHERE m.game_mode = gm.id) AS match_count
            FROM game_modes gm
            WHERE gm.is_visible = 0
            ORDER BY gm.display_name
        ')->fetchAll();
    }

    public function deleteOrphanedGameMode(string $id): bool
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('DELETE FROM game_modes WHERE id = :id AND is_visible = 0');
        $stmt->execute(['id' => $id]);
        return $stmt->rowCount() > 0;
    }

    public function deleteAllOrphanedGameModes(): int
    {
        $db = Database::getConnection();
        $stmt = $db->exec('DELETE FROM game_modes WHERE is_visible = 0');
        return (int) $stmt;
    }
}
