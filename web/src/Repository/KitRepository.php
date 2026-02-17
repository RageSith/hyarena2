<?php

namespace App\Repository;

use App\Database;

class KitRepository
{
    public function upsert(array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO kits (id, display_name, description, icon, is_visible, shown)
            VALUES (:id, :display_name, :description, :icon, 1, 1)
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                description = VALUES(description),
                is_visible = 1
        ');
        $stmt->execute([
            'id' => $data['id'],
            'display_name' => $data['display_name'],
            'description' => $data['description'] ?? null,
            'icon' => $data['icon'] ?? null,
        ]);
    }

    /**
     * Ensures a kit row exists by ID. Inserts a placeholder if missing.
     */
    public function ensureExists(string $id): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT IGNORE INTO kits (id, display_name)
            VALUES (:id, :display_name)
        ');
        $stmt->execute([
            'id' => $id,
            'display_name' => $id,
        ]);
    }

    public function markAllInvisible(): void
    {
        $db = Database::getConnection();
        $db->exec('UPDATE kits SET is_visible = 0');
    }

    public function getAll(): array
    {
        $db = Database::getConnection();
        return $db->query('SELECT * FROM kits WHERE is_visible = 1 AND shown = 1 ORDER BY display_name')->fetchAll();
    }

    public function findById(string $id): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM kits WHERE id = :id');
        $stmt->execute(['id' => $id]);
        return $stmt->fetch() ?: null;
    }

    public function getVisibleWithStats(): array
    {
        $db = Database::getConnection();

        // Get total matches across all kits for pick rate calculation
        $totalMatches = (int) $db->query('
            SELECT COALESCE(SUM(matches_played), 0) FROM player_kit_stats
        ')->fetchColumn();

        if ($totalMatches === 0) {
            return [];
        }

        $stmt = $db->prepare('
            SELECT
                k.id,
                k.display_name,
                k.description,
                k.icon,
                SUM(pks.matches_played) AS total_matches,
                COUNT(DISTINCT pks.player_uuid) AS unique_players,
                ROUND(SUM(pks.matches_played) / :total * 100, 1) AS pick_rate,
                CASE WHEN SUM(pks.matches_played) = 0 THEN 0
                     ELSE ROUND(SUM(pks.matches_won) / SUM(pks.matches_played) * 100, 1) END AS avg_win_rate,
                CASE WHEN SUM(pks.pvp_deaths) = 0 THEN SUM(pks.pvp_kills)
                     ELSE ROUND(SUM(pks.pvp_kills) / SUM(pks.pvp_deaths), 2) END AS avg_pvp_kd,
                SUM(pks.pvp_kills) AS total_pvp_kills,
                SUM(pks.damage_dealt) AS total_damage_dealt
            FROM kits k
            JOIN player_kit_stats pks ON pks.kit_id = k.id
            WHERE k.is_visible = 1 AND k.shown = 1
            GROUP BY k.id, k.display_name, k.description, k.icon
            HAVING SUM(pks.matches_played) > 0
            ORDER BY total_matches DESC
        ');
        $stmt->execute(['total' => $totalMatches]);
        return $stmt->fetchAll();
    }

    public function getCount(): int
    {
        $db = Database::getConnection();
        return (int) $db->query('SELECT COUNT(*) FROM kits WHERE is_visible = 1 AND shown = 1')->fetchColumn();
    }
}
