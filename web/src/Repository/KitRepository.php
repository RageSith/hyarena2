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

    public function getCount(): int
    {
        $db = Database::getConnection();
        return (int) $db->query('SELECT COUNT(*) FROM kits')->fetchColumn();
    }
}
