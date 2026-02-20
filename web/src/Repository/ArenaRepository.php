<?php

namespace App\Repository;

use App\Database;

class ArenaRepository
{
    public function upsert(array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO arenas (id, display_name, description, game_mode, world_name, min_players, max_players, icon, is_visible, shown)
            VALUES (:id, :display_name, :description, :game_mode, :world_name, :min_players, :max_players, :icon, 1, 1)
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                description = VALUES(description),
                game_mode = VALUES(game_mode),
                world_name = VALUES(world_name),
                min_players = VALUES(min_players),
                max_players = VALUES(max_players),
                is_visible = 1
        ');
        $stmt->execute([
            'id' => $data['id'],
            'display_name' => $data['display_name'],
            'description' => $data['description'] ?? null,
            'game_mode' => $data['game_mode'],
            'world_name' => $data['world_name'],
            'min_players' => $data['min_players'] ?? 2,
            'max_players' => $data['max_players'] ?? 2,
            'icon' => $data['icon'] ?? null,
        ]);
    }

    public function markAllInvisible(): void
    {
        $db = Database::getConnection();
        $db->exec('UPDATE arenas SET is_visible = 0');
    }

    public function getAll(): array
    {
        $db = Database::getConnection();
        return $db->query('SELECT * FROM arenas WHERE is_visible = 1 AND shown = 1 ORDER BY display_name')->fetchAll();
    }

    public function findById(string $id): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM arenas WHERE id = :id');
        $stmt->execute(['id' => $id]);
        return $stmt->fetch() ?: null;
    }

    public function getCount(): int
    {
        $db = Database::getConnection();
        return (int) $db->query('SELECT COUNT(*) FROM arenas WHERE is_visible = 1 AND shown = 1')->fetchColumn();
    }

    public function getAllAdmin(): array
    {
        $db = Database::getConnection();
        return $db->query('SELECT * FROM arenas ORDER BY is_visible DESC, display_name')->fetchAll();
    }

    public function updateIcon(string $id, ?string $icon): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('UPDATE arenas SET icon = :icon WHERE id = :id');
        $stmt->execute(['icon' => $icon, 'id' => $id]);
    }

    public function updateShown(string $id, bool $shown): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('UPDATE arenas SET shown = :shown WHERE id = :id');
        $stmt->execute(['shown' => $shown ? 1 : 0, 'id' => $id]);
    }
}
