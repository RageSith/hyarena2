<?php

namespace App\Repository;

use App\Database;

class GameModeRepository
{
    public function upsert(array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO game_modes (id, display_name, description)
            VALUES (:id, :display_name, :description)
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                description = VALUES(description)
        ');
        $stmt->execute([
            'id' => $data['id'],
            'display_name' => $data['display_name'],
            'description' => $data['description'] ?? null,
        ]);
    }

    public function getAll(): array
    {
        $db = Database::getConnection();
        return $db->query('SELECT * FROM game_modes ORDER BY display_name')->fetchAll();
    }
}
