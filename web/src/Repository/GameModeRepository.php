<?php

namespace App\Repository;

use App\Database;

class GameModeRepository
{
    public function upsert(array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO game_modes (id, display_name, description, is_visible)
            VALUES (:id, :display_name, :description, 1)
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                description = VALUES(description),
                is_visible = 1
        ');
        $stmt->execute([
            'id' => $data['id'],
            'display_name' => $data['display_name'],
            'description' => $data['description'] ?? null,
        ]);
    }

    public function markAllInvisible(): void
    {
        $db = Database::getConnection();
        $db->exec('UPDATE game_modes SET is_visible = 0');
    }

    public function getAll(): array
    {
        $db = Database::getConnection();
        return $db->query('SELECT * FROM game_modes WHERE is_visible = 1 ORDER BY display_name')->fetchAll();
    }
}
