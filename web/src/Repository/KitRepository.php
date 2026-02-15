<?php

namespace App\Repository;

use App\Database;

class KitRepository
{
    public function upsert(array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO kits (id, display_name, description, icon)
            VALUES (:id, :display_name, :description, :icon)
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                description = VALUES(description),
                icon = VALUES(icon)
        ');
        $stmt->execute([
            'id' => $data['id'],
            'display_name' => $data['display_name'],
            'description' => $data['description'] ?? null,
            'icon' => $data['icon'] ?? null,
        ]);
    }

    public function getAll(): array
    {
        $db = Database::getConnection();
        return $db->query('SELECT * FROM kits ORDER BY display_name')->fetchAll();
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
            WHERE k.is_visible = 1
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
        return (int) $db->query('SELECT COUNT(*) FROM kits')->fetchColumn();
    }
}
